package com.greninjaop.mailorganizer.data.tasks

import android.content.Intent
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.data.http.HttpTransport
import com.greninjaop.mailorganizer.data.http.TransportResponse
import java.io.IOException
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.core.actions.ActionConfidence
import com.greninjaop.mailorganizer.core.actions.ActionSource
import com.greninjaop.mailorganizer.core.actions.ActionUrgency
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.core.integrations.IntegrationAdapter
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.core.integrations.IntegrationStatus
import com.greninjaop.mailorganizer.core.integrations.PermissionDescription
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.AuthResult
import com.greninjaop.mailorganizer.data.auth.GoogleAuthState
import com.greninjaop.mailorganizer.data.integrations.RealTasksIntegrationAdapter
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.local.IntegrationStateRecord
import com.greninjaop.mailorganizer.data.repository.IntegrationStateRepository
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager
import com.greninjaop.mailorganizer.domain.tasks.CreateTaskActionExecutor
import java.nio.file.Files
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 16 unit tests: JSON parsing, request building, error mapping,
 * idempotency, the file-backed execution store, the HTTP client against
 * a local fake Tasks server, proposal building, the executor, and the
 * adapter's honest status reporting.
 *
 * Everything is pure JVM — no android.jar stubs are *called*
 * (android.content.Intent appears only in uncalled fake signatures),
 * no Robolectric.
 */
class TasksPhase16Test {

    private val http = GoogleTasksHttp()
    private var server: FakeTasksServer? = null

    @After
    fun tearDown() {
        server?.stop()
        server = null
    }

    // ---------- JsonEscape (parsing itself is covered by core.JsonParser's tests) ----------

    @Test
    fun `jsonEscape round-trips through the shared parser`() {
        val raw = "Quote \"hi\"\nBack\\slash\tTab"
        val parsed = JsonParser.parse("""{"v":"${jsonEscape(raw)}"}""")?.string("v")
        assertEquals(raw, parsed)
    }

    // ---------- Task lists / task parsing ----------

    @Test
    fun `parseTaskListsResponse parses real-shaped body`() {
        val body = """{"kind":"tasks#taskLists","etag":"x","items":[""" +
            """{"kind":"tasks#taskList","id":"abc123","title":"My Tasks","updated":"2026-10-01T00:00:00.000Z"},""" +
            """{"kind":"tasks#taskList","id":"def456","title":"","updated":"2026-10-02T00:00:00.000Z"}]}"""
        val lists = http.parseTaskListsResponse(body)
        assertNotNull(lists)
        assertEquals(2, lists!!.size)
        assertEquals("abc123", lists[0].id)
        assertEquals("My Tasks", lists[0].title)
        assertEquals("Untitled list", lists[1].displayTitle)
    }

    @Test
    fun `parseTaskListsResponse tolerates missing items`() {
        assertEquals(emptyList<GoogleTaskList>(), http.parseTaskListsResponse("""{"kind":"tasks#taskLists"}"""))
        assertNull(http.parseTaskListsResponse("not json"))
    }

    @Test
    fun `parseTaskListsResponse skips id-less entries`() {
        val body = """{"items":[{"title":"No id"},{"id":"ok","title":"Fine"}]}"""
        val lists = http.parseTaskListsResponse(body)!!
        assertEquals(1, lists.size)
        assertEquals("ok", lists[0].id)
    }

    @Test
    fun `parseTask parses full task`() {
        val body = """{"kind":"tasks#task","id":"task1","title":"Buy milk","notes":"line1\nline2",""" +
            """"status":"completed","due":"2026-10-18T00:00:00.000Z","updated":"2026-10-11T10:00:00.000Z"}"""
        val task = http.parseTask(body)!!
        assertEquals("task1", task.id)
        assertEquals("Buy milk", task.title)
        assertEquals("line1\nline2", task.notes)
        assertEquals(TaskStatus.COMPLETED, task.status)
        assertNotNull(task.dueEpochMs)
    }

    @Test
    fun `parseTask defaults missing status to needsAction`() {
        val task = http.parseTask("""{"id":"t","title":"x"}""")!!
        assertEquals(TaskStatus.NEEDS_ACTION, task.status)
        assertNull(task.dueEpochMs)
        assertNull(task.notes)
    }

    @Test
    fun `parseTask rejects id-less or malformed bodies`() {
        assertNull(http.parseTask("""{"title":"no id"}"""))
        assertNull(http.parseTask("garbage"))
    }

    // ---------- Request bodies ----------

    @Test
    fun `buildInsertBody escapes and appends dedupe marker`() {
        val draft = TaskDraft(
            title = "Quote \"test\"",
            notes = "some notes",
            dueEpochMs = null,
            idempotencyKey = "abc123",
        )
        val body = http.buildInsertBody(draft)
        assertTrue(body.contains(""""title":"Quote \"test\"""""))
        assertTrue(body.contains("[mailstack:task:abc123]"))
        assertFalse(body.contains("\"due\""))
    }

    @Test
    fun `buildInsertBody includes due when present`() {
        val body = http.buildInsertBody(
            TaskDraft("t", null, 1_791_000_000_000L, "k"),
        )
        assertTrue(body.contains(""""due":"2026-10-"""))
    }

    @Test
    fun `buildPatchBody covers rename reschedule complete`() {
        val body = http.buildPatchBody(
            TaskPatch(title = "New", dueEpochMs = 1_791_000_000_000L, completed = true),
        )
        assertTrue(body.contains(""""title":"New""""))
        assertTrue(body.contains(""""status":"completed""""))
        assertTrue(body.contains(""""due":"2026-10-"""))
    }

    @Test
    fun `buildPatchBody supports clearDue and reopen`() {
        val body = http.buildPatchBody(TaskPatch(clearDue = true, completed = false))
        assertTrue(body.contains(""""due":null"""))
        assertTrue(body.contains(""""status":"needsAction""""))
    }

    // ---------- Error mapping ----------

    @Test
    fun `mapHttpError maps 401 to Authentication`() {
        val e = http.mapHttpError(401, """{"error":{"message":"Invalid Credentials"}}""")
        assertTrue(e is MoError.Authentication)
    }

    @Test
    fun `mapHttpError maps 403 insufficient scope to PermissionDenied`() {
        val body = """{"error":{"errors":[{"reason":"insufficientPermissions","message":"Insufficient Permission"}],"code":403,"message":"Insufficient Permission"}}"""
        val e = http.mapHttpError(403, body)
        assertTrue(e is MoError.PermissionDenied)
        assertEquals("https://www.googleapis.com/auth/tasks", (e as MoError.PermissionDenied).permission)
    }

    @Test
    fun `mapHttpError maps other 403 to Api`() {
        val e = http.mapHttpError(403, """{"error":{"message":"Forbidden"}}""")
        assertTrue(e is MoError.Api)
        assertEquals(403, (e as MoError.Api).code)
    }

    @Test
    fun `mapHttpError maps 404 429 500`() {
        assertTrue(http.mapHttpError(404, "") is MoError.Api)
        val rl = http.mapHttpError(429, "", retryAfterSeconds = 30)
        assertTrue(rl is MoError.RateLimited)
        assertEquals(30L, (rl as MoError.RateLimited).retryAfterSeconds)
        val srv = http.mapHttpError(500, "")
        assertTrue(srv is MoError.Api)
        assertEquals(500, (srv as MoError.Api).code)
    }

    // ---------- Due date handling ----------

    @Test
    fun `formatTasksDue preserves device date in UTC`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
            // 2026-10-18 00:00 IST.
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"), Locale.US).apply {
                clear(); set(2026, Calendar.OCTOBER, 18, 0, 0, 0)
            }
            assertEquals("2026-10-18T00:00:00.000Z", http.formatTasksDue(cal.timeInMillis))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `parseTasksDue parses Z and offsets`() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
            clear(); set(2026, Calendar.OCTOBER, 18, 0, 0, 0)
        }.timeInMillis
        assertEquals(utc, http.parseTasksDue("2026-10-18T00:00:00.000Z"))
        assertEquals(utc, http.parseTasksDue("2026-10-18T05:30:00+05:30"))
        assertNull(http.parseTasksDue("not-a-date"))
    }

    @Test
    fun `encodePath keeps default alias intact`() {
        assertEquals("@default", http.encodePath("@default"))
        assertEquals("my%20list", http.encodePath("my list"))
    }

    // ---------- TaskDedupe ----------

    @Test
    fun `idempotencyKey is stable and scoped`() {
        val a = TaskDedupe.idempotencyKey("google_a", "@default", "m1|REMINDER|k1")
        val b = TaskDedupe.idempotencyKey("google_a", "@default", "m1|REMINDER|k1")
        assertEquals(a, b)
        assertEquals(16, a.length)
        assertNotEquals(a, TaskDedupe.idempotencyKey("google_b", "@default", "m1|REMINDER|k1"))
        assertNotEquals(a, TaskDedupe.idempotencyKey("google_a", "other", "m1|REMINDER|k1"))
        assertNotEquals(a, TaskDedupe.idempotencyKey("google_a", "@default", "m2|REMINDER|k1"))
    }

    @Test
    fun `marker round-trips through notes`() {
        val key = "9f2ac41bd77e03c1"
        val marker = TaskDedupe.marker(key)
        assertEquals(key, TaskDedupe.extractKey("Some notes\n$marker"))
        assertEquals(key, TaskDedupe.extractKey(marker))
        assertTrue(TaskDedupe.hasKey("notes\n$marker", key))
        assertFalse(TaskDedupe.hasKey("notes", key))
        assertFalse(TaskDedupe.hasKey(null, key))
        assertNull(TaskDedupe.extractKey("no marker here"))
    }

    @Test
    fun `findByKey locates the marked task`() {
        val key = "abc"
        val tasks = listOf(
            GoogleTask("1", "a", "plain", TaskStatus.NEEDS_ACTION, null),
            GoogleTask("2", "b", "x\n${TaskDedupe.marker(key)}", TaskStatus.NEEDS_ACTION, null),
        )
        assertEquals("2", TaskDedupe.findByKey(tasks, key)?.id)
        assertNull(TaskDedupe.findByKey(tasks, "other"))
    }

    // ---------- TaskExecutionStore ----------

    @Test
    fun `store defaults selected list to default alias`() {
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val store = TaskExecutionStore(dir)
        assertEquals("@default", store.getSelectedTaskListId("google_a"))
    }

    @Test
    fun `store round-trips executions and selection`() {
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val store = TaskExecutionStore(dir)
        store.setSelectedTaskListId("google_a", "list-9")
        assertEquals("list-9", store.getSelectedTaskListId("google_a"))
        // Other accounts unaffected.
        assertEquals("@default", store.getSelectedTaskListId("google_b"))

        val rec = TaskExecutionRecord("m1|REMINDER|k1", "list-9", "gtask-1", "key1", "Title", 123L)
        store.recordExecution("google_a", rec)
        assertEquals(rec, store.findExecution("google_a", "m1|REMINDER|k1"))
        assertEquals(rec, store.findByTaskId("google_a", "gtask-1"))
        assertNull(store.findExecution("google_b", "m1|REMINDER|k1"))

        // Survives a fresh instance (file-backed).
        val store2 = TaskExecutionStore(dir)
        assertEquals("list-9", store2.getSelectedTaskListId("google_a"))
        assertEquals("gtask-1", store2.findExecution("google_a", "m1|REMINDER|k1")?.googleTaskId)
    }

    @Test
    fun `store clearAccount is account-isolated`() {
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val store = TaskExecutionStore(dir)
        store.recordExecution("google_a", TaskExecutionRecord("k", "l", "t1", "key", "T", 1L))
        store.recordExecution("google_b", TaskExecutionRecord("k", "l", "t2", "key", "T", 1L))
        store.clearAccount("google_a")
        assertNull(store.findExecution("google_a", "k"))
        assertNotNull(store.findExecution("google_b", "k"))
    }

    @Test
    fun `store tolerates corrupt files`() {
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val store = TaskExecutionStore(dir)
        store.setSelectedTaskListId("google_a", "list-9")
        // Corrupt the file directly.
        dir.listFiles()!!.first { it.name.contains("google_a") }.writeText("{corrupt")
        val fresh = TaskExecutionStore(dir)
        assertEquals("@default", fresh.getSelectedTaskListId("google_a"))
        assertNull(fresh.findExecution("google_a", "k"))
    }

    // ---------- Fake-server HTTP tests ----------

    private fun startServer(): FakeTasksServer {
        val s = FakeTasksServer()
        server = s
        return s
    }

    private fun clientFor(s: FakeTasksServer) = GoogleTasksHttp(s.baseUrl(), s.transport)

    @Test
    fun `listTaskLists sends bearer token and parses`() = runBlocking {
        val s = startServer()
        s.routes["GET /users/@me/lists"] = {
            TransportResponse(200, """{"items":[{"id":"l1","title":"Personal"}]}""")
        }
        val client = clientFor(s)
        val result = client.listTaskLists("tok-123")
        assertTrue(result is MoResult.Success)
        assertEquals("l1", (result as MoResult.Success).value[0].id)
        assertEquals("Bearer tok-123", s.recorded.single().authHeader)
    }

    @Test
    fun `insertTask posts title notes due and marker`() = runBlocking {
        val s = startServer()
        s.routes["GET /lists/@default/tasks"] = {
            TransportResponse(200, """{"items":[]}""")
        }
        var postedBody = ""
        s.routes["POST /lists/@default/tasks"] = {
            postedBody = s.recorded.last().body
            TransportResponse(200, """{"id":"task-1","title":"Do it","status":"needsAction"}""")
        }
        val client = clientFor(s)
        val draft = TaskDraft("Do it", "notes here", null, "key9")
        val result = client.insertTask("tok", "@default", draft)
        assertTrue(result is MoResult.Success)
        assertEquals("task-1", (result as MoResult.Success).value.id)
        assertTrue(postedBody.contains(""""title":"Do it""""))
        assertTrue(postedBody.contains("notes here"))
        assertTrue(postedBody.contains("[mailstack:task:key9]"))
    }

    @Test
    fun `insertTask maps 401 to Authentication`() = runBlocking {
        val s = startServer()
        s.routes["POST /lists/@default/tasks"] = {
            TransportResponse(401, """{"error":{"message":"Invalid Credentials"}}""")
        }
        val result = clientFor(s).insertTask("bad", "@default", TaskDraft("t", null, null, "k"))
        assertTrue(result is MoResult.Failure)
        assertTrue((result as MoResult.Failure).error is MoError.Authentication)
    }

    @Test
    fun `patchTask uses method override header`() = runBlocking {
        val s = startServer()
        s.routes["POST /lists/@default/tasks/t1"] = {
            TransportResponse(200, """{"id":"t1","title":"Renamed","status":"completed"}""")
        }
        val result = clientFor(s).patchTask(
            "tok", "@default", "t1", TaskPatch(title = "Renamed", completed = true),
        )
        assertTrue(result is MoResult.Success)
        val rec = s.recorded.single()
        assertEquals("POST", rec.method)
        // The override header is what Google's frontend honors as PATCH.
        assertEquals("PATCH", rec.methodOverride)
        assertTrue(rec.body.contains(""""status":"completed""""))
    }

    @Test
    fun `deleteTask succeeds on 204`() = runBlocking {
        val s = startServer()
        s.routes["DELETE /lists/@default/tasks/t1"] = { TransportResponse(204, "") }
        val result = clientFor(s).deleteTask("tok", "@default", "t1")
        assertTrue(result is MoResult.Success)
    }

    @Test
    fun `client reports network failure honestly`() = runBlocking {
        val client = GoogleTasksHttp(
            "http://fake.tasks",
            HttpTransport { _, _, _, _ -> throw IOException("simulated network failure") },
        )
        val result = client.listTaskLists("tok")
        assertTrue(result is MoResult.Failure)
        assertTrue((result as MoResult.Failure).error is MoError.Network)
    }

    // ---------- TasksRepository ----------

    private class FakeAuthRepository(
        private val tokens: Map<String, String?> = emptyMap(),
    ) : AuthRepository {
        val tokenRequests = mutableListOf<String>()
        override fun observeAuthStates(): Flow<Map<AccountId, GoogleAuthState>> =
            flowOf(emptyMap<AccountId, GoogleAuthState>())
        override fun observeAuthState(accountId: AccountId): Flow<GoogleAuthState> =
            flowOf(GoogleAuthState.Disconnected)
        override suspend fun getValidAccessToken(accountId: AccountId): String? {
            tokenRequests.add(accountId.value)
            return tokens[accountId.value]
        }
        override suspend fun handleOAuthResult(data: Intent?): AuthResult =
            throw UnsupportedOperationException()
        override fun createOAuthIntent(additionalScopes: List<String>): Intent =
            throw UnsupportedOperationException()
        override fun createIncrementalAuthIntent(
            accountId: AccountId,
            additionalScopes: List<String>,
        ): Intent = throw UnsupportedOperationException()
        override fun observeOAuthResults(): Flow<AuthResult> = flowOf()
        override suspend fun disconnect(accountId: AccountId) = Unit
        override suspend fun isConnected(accountId: AccountId): Boolean =
            tokens[accountId.value] != null
    }

    private fun repositoryFor(
        s: FakeTasksServer,
        tokens: Map<String, String?>,
        dir: java.io.File? = null,
    ): TasksRepository {
        val store = dir?.let { TaskExecutionStore(it) }
        return TasksRepository(FakeAuthRepository(tokens), GoogleTasksHttp(s.baseUrl(), s.transport), store)
    }

    @Test
    fun `createTask is idempotent when marker already present`() = runBlocking {
        val s = startServer()
        val key = TaskDedupe.idempotencyKey("google_a", "@default", "m1|REMINDER|k1")
        val marker = TaskDedupe.marker(key)
        s.routes["GET /lists/@default/tasks"] = {
            TransportResponse(200, """{"items":[{"id":"existing-7","title":"Submit","notes":"desc\n$marker","status":"needsAction"}]}""",
            )
        }
        s.routes["POST /lists/@default/tasks"] = {
            TransportResponse(200, """{"id":"should-not-happen"}""")
        }
        val repo = repositoryFor(s, mapOf("google_a" to "tok-a"))
        val result = repo.createTask(
            AccountId("google_a"), "@default", "m1|REMINDER|k1", "Submit", "desc", null,
        )
        assertTrue(result is MoResult.Success)
        assertEquals("existing-7", (result as MoResult.Success).value.id)
        // No POST happened — the duplicate was avoided.
        assertTrue(s.recorded.none { it.method == "POST" })
    }

    @Test
    fun `createTask recovers after a lost response without duplicating`() = runBlocking {
        val s = startServer()
        val key = TaskDedupe.idempotencyKey("google_a", "@default", "m1|REMINDER|k1")
        val marker = TaskDedupe.marker(key)
        var listCalls = 0
        s.routes["GET /lists/@default/tasks"] = {
            listCalls++
            // First scan: empty. Recovery scan: the task is there
            // (Google created it; the response was lost).
            val items = if (listCalls == 1) "[]" else
                """[{"id":"recovered-3","title":"Submit","notes":"d\n$marker","status":"needsAction"}]"""
            TransportResponse(200, """{"items":$items}""")
        }
        s.routes["POST /lists/@default/tasks"] = {
            // Simulate the response being lost: close without answering.
            throw IOException("simulated lost response")
        }
        val repo = repositoryFor(s, mapOf("google_a" to "tok-a"))
        val result = repo.createTask(
            AccountId("google_a"), "@default", "m1|REMINDER|k1", "Submit", "d", null,
        )
        assertTrue(result is MoResult.Success)
        assertEquals("recovered-3", (result as MoResult.Success).value.id)
    }

    @Test
    fun `createTask maps 403 insufficient scope to PermissionDenied`() = runBlocking {
        val s = startServer()
        s.routes["GET /lists/@default/tasks"] = { TransportResponse(200, """{"items":[]}""") }
        s.routes["POST /lists/@default/tasks"] = {
            TransportResponse(403, """{"error":{"errors":[{"reason":"insufficientPermissions"}],"message":"x"}}""")
        }
        val repo = repositoryFor(s, mapOf("google_a" to "tok-a"))
        val result = repo.createTask(
            AccountId("google_a"), "@default", "m1|REMINDER|k1", "T", null, null,
        )
        assertTrue(result is MoResult.Failure)
        assertTrue((result as MoResult.Failure).error is MoError.PermissionDenied)
    }

    @Test
    fun `createTask without token fails as Authentication`() = runBlocking {
        val s = startServer()
        val repo = repositoryFor(s, emptyMap())
        val result = repo.createTask(
            AccountId("google_a"), "@default", "m1|REMINDER|k1", "T", null, null,
        )
        assertTrue(result is MoResult.Failure)
        assertTrue((result as MoResult.Failure).error is MoError.Authentication)
        assertTrue(s.recorded.isEmpty()) // no network attempted
    }

    @Test
    fun `repository keeps accounts isolated`() = runBlocking {
        val s = startServer()
        s.routes["GET /lists/@default/tasks"] = { TransportResponse(200, """{"items":[]}""") }
        s.routes["POST /lists/@default/tasks"] = {
            TransportResponse(200, """{"id":"t-b","title":"x","status":"needsAction"}""")
        }
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val repo = repositoryFor(s, mapOf("google_a" to "tok-A", "google_b" to "tok-B"), dir)
        val result = repo.createTask(
            AccountId("google_b"), "@default", "m9|REMINDER|k9", "x", null, null,
        )
        assertTrue(result is MoResult.Success)
        // Account B's token was used, never A's.
        assertTrue(s.recorded.all { it.authHeader == "Bearer tok-B" })
        // Execution recorded under B only.
        assertNotNull(repo.findExecution(AccountId("google_b"), "m9|REMINDER|k9"))
        assertNull(repo.findExecution(AccountId("google_a"), "m9|REMINDER|k9"))
    }

    @Test
    fun `update and delete go through per-account token`() = runBlocking {
        val s = startServer()
        s.routes["POST /lists/L/tasks/t1"] = {
            TransportResponse(200, """{"id":"t1","title":"x","status":"completed"}""")
        }
        s.routes["DELETE /lists/L/tasks/t1"] = { TransportResponse(204, "") }
        val repo = repositoryFor(s, mapOf("google_a" to "tok-a"))
        val updated = repo.setTaskCompleted(AccountId("google_a"), "L", "t1", true)
        assertTrue(updated is MoResult.Success)
        assertEquals(TaskStatus.COMPLETED, (updated as MoResult.Success).value.status)
        val deleted = repo.deleteTask(AccountId("google_a"), "L", "t1")
        assertTrue(deleted is MoResult.Success)
        assertTrue(s.recorded.all { it.authHeader == "Bearer tok-a" })
    }

    @Test
    fun `probeAccess distinguishes auth scope and network`() = runBlocking {
        suspend fun probeWith(code: Int, body: String): MoResult<Unit> {
            val s = startServer()
            s.routes["GET /users/@me/lists"] = { TransportResponse(code, body) }
            val repo = repositoryFor(s, mapOf("google_a" to "tok"))
            val r = repo.probeAccess(AccountId("google_a"))
            s.stop(); server = null
            return r
        }
        assertTrue(probeWith(200, """{"items":[]}""") is MoResult.Success)
        assertTrue((probeWith(401, "{}") as MoResult.Failure).error is MoError.Authentication)
        val denied = probeWith(403, """{"error":{"errors":[{"reason":"insufficientPermissions"}]}}""")
        assertTrue((denied as MoResult.Failure).error is MoError.PermissionDenied)

        val offline = GoogleTasksHttp(
            "http://fake.tasks",
            HttpTransport { _, _, _, _ -> throw IOException("simulated network failure") },
        )
        val repo = TasksRepository(FakeAuthRepository(mapOf("google_a" to "t")), offline)
        val r = repo.probeAccess(AccountId("google_a"))
        assertTrue((r as MoResult.Failure).error is MoError.Network)
    }

    // ---------- TaskProposal ----------

    private fun candidate(
        accountId: String = "google_a",
        title: String = "Submit application",
        description: String = "Submit the internship application soon.",
        due: Long? = 1_791_000_000_000L,
        effect: ExternalEffect = ExternalEffect.TASKS,
        messageId: String = "m1",
        targetKey: String = "k1",
    ) = ActionCandidate(
        id = "$messageId|REMINDER|$targetKey",
        accountId = accountId,
        messageId = messageId,
        threadId = "t1",
        actionType = ActionType.REMINDER,
        title = title,
        description = description,
        urgency = ActionUrgency.NORMAL,
        source = ActionSource.CLASSIFICATION,
        confidence = ActionConfidence.HIGH,
        explanation = "test",
        signals = emptyList(),
        externalEffect = effect,
        missingInfo = emptyList(),
        dueDateEpochMs = due,
        targetKey = targetKey,
        version = 1,
    )

    @Test
    fun `proposal keeps explicit title and honest notes`() {
        val p = buildTaskProposal(candidate(), "@default", "My Tasks")
        assertEquals("Submit application", p.title)
        assertTrue(p.notes!!.contains("Source: Mail Organizer"))
        assertTrue(p.notes.contains("Due:"))
        assertEquals(1_791_000_000_000L, p.dueEpochMs)
        assertEquals("m1|REMINDER|k1", p.actionKey)
        assertTrue(p.isExecutable)
        // No invented content, no ids, no links.
        assertFalse(p.notes.contains("m1"))
        assertFalse(p.notes.contains("http"))
    }

    @Test
    fun `proposal falls back honestly when title is blank`() {
        val p = buildTaskProposal(candidate(title = "", description = "Pay the fee"), "L", null)
        assertEquals("Pay the fee", p.title)
        val p2 = buildTaskProposal(candidate(title = "", description = ""), "L", null)
        assertEquals("Action from Mail Organizer", p2.title)
    }

    @Test
    fun `proposal never invents a due date`() {
        val p = buildTaskProposal(candidate(due = null), "L", null)
        assertNull(p.dueEpochMs)
        assertFalse(p.notes!!.contains("Due:"))
    }

    @Test
    fun `proposal flags missing account and list`() {
        val p = buildTaskProposal(candidate(accountId = ""), "", null)
        assertFalse(p.isExecutable)
        assertTrue(p.problems().size >= 2)
    }

    // ---------- CreateTaskActionExecutor ----------

    @Test
    fun `executor supports only REMINDER`() {
        val exec = CreateTaskActionExecutor(
            TasksRepository(FakeAuthRepository()),
            managerWith(connected = true),
        )
        assertTrue(exec.supports(ActionType.REMINDER))
        assertFalse(exec.supports(ActionType.MEETING))
        assertFalse(exec.supports(ActionType.DEADLINE))
    }

    @Test
    fun `executor validate rejects internal or malformed candidates`() {
        val exec = CreateTaskActionExecutor(
            TasksRepository(FakeAuthRepository()),
            managerWith(connected = true),
        )
        // Internal suggestion (NONE) must never reach the Tasks API.
        assertTrue(exec.validate(candidate(effect = ExternalEffect.NONE)) is MoResult.Failure)
        assertTrue(exec.validate(candidate(accountId = "")) is MoResult.Failure)
        assertTrue(exec.validate(candidate(title = "", description = "")) is MoResult.Failure)
        assertTrue(exec.validate(candidate()) is MoResult.Success)
    }

    @Test
    fun `executor refuses when integration not capable`() = runBlocking {
        val s = startServer()
        val exec = CreateTaskActionExecutor(
            TasksRepository(FakeAuthRepository(mapOf("google_a" to "t")), GoogleTasksHttp(s.baseUrl())),
            managerWith(connected = false),
        )
        val result = exec.execute(candidate())
        assertTrue(result is MoResult.Failure)
        assertTrue(s.recorded.isEmpty()) // nothing external happened
    }

    @Test
    fun `executor creates task after confirmation and returns receipt`() = runBlocking {
        val s = startServer()
        s.routes["GET /lists/@default/tasks"] = { TransportResponse(200, """{"items":[]}""") }
        s.routes["POST /lists/@default/tasks"] = {
            TransportResponse(200, """{"id":"task-9","title":"Submit application","status":"needsAction"}""")
        }
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val repo = TasksRepository(
            FakeAuthRepository(mapOf("google_a" to "tok-a")),
            GoogleTasksHttp(s.baseUrl(), s.transport),
            TaskExecutionStore(dir),
        )
        val exec = CreateTaskActionExecutor(repo, managerWith(connected = true))
        val result = exec.execute(candidate())
        assertTrue(result is MoResult.Success)
        val receipt = (result as MoResult.Success).value
        assertEquals("Google Tasks", receipt.executorName)
        assertEquals("task-9", receipt.externalId)
        assertTrue(receipt.summary.contains("Submit application"))
        // Execution recorded for future update/delete.
        assertNotNull(repo.findExecution(AccountId("google_a"), "m1|REMINDER|k1"))
    }

    // ---------- RealTasksIntegrationAdapter ----------

    private fun adapterFor(
        s: FakeTasksServer?,
        tokens: Map<String, String?>,
        dir: java.io.File? = null,
    ): RealTasksIntegrationAdapter {
        val http = if (s == null) {
            GoogleTasksHttp(
                "http://fake.tasks",
                HttpTransport { _, _, _, _ -> throw IOException("simulated network failure") },
            )
        } else {
            GoogleTasksHttp(s.baseUrl(), s.transport)
        }
        return RealTasksIntegrationAdapter(
            authRepository = FakeAuthRepository(tokens),
            tasksHttp = http,
            executionStore = dir?.let { TaskExecutionStore(it) },
        )
    }

    @Test
    fun `adapter snapshot is DISCONNECTED without account`() = runBlocking {
        val adapter = adapterFor(null, emptyMap())
        val snap = adapter.snapshot(null)
        assertEquals(IntegrationStatus.DISCONNECTED, snap.status)
        assertTrue(snap.usableCapabilities.isEmpty())
    }

    @Test
    fun `adapter snapshot is AUTH_REQUIRED without google auth`() = runBlocking {
        val adapter = adapterFor(null, emptyMap())
        val snap = adapter.snapshot("google_a")
        assertEquals(IntegrationStatus.AUTH_REQUIRED, snap.status)
    }

    @Test
    fun `adapter snapshot is CONNECTED only on proven access`() = runBlocking {
        val s = startServer()
        s.routes["GET /users/@me/lists"] = { TransportResponse(200, """{"items":[]}""") }
        val snap = adapterFor(s, mapOf("google_a" to "tok")).snapshot("google_a")
        assertEquals(IntegrationStatus.CONNECTED, snap.status)
        assertTrue(snap.isCapable(IntegrationCapability.CREATE_TASK))
        assertTrue(snap.isCapable(IntegrationCapability.READ_TASK_LISTS))
    }

    @Test
    fun `adapter snapshot reports PERMISSION_REQUIRED on missing scope`() = runBlocking {
        val s = startServer()
        s.routes["GET /users/@me/lists"] = {
            TransportResponse(403, """{"error":{"errors":[{"reason":"insufficientPermissions"}]}}""")
        }
        val snap = adapterFor(s, mapOf("google_a" to "tok")).snapshot("google_a")
        assertEquals(IntegrationStatus.PERMISSION_REQUIRED, snap.status)
        assertTrue(snap.usableCapabilities.isEmpty())
        assertTrue(snap.statusReason.contains("Tasks permission"))
    }

    @Test
    fun `adapter snapshot reports OFFLINE on network failure`() = runBlocking {
        val snap = adapterFor(null, mapOf("google_a" to "tok")).snapshot("google_a")
        assertEquals(IntegrationStatus.OFFLINE, snap.status)
        assertTrue(snap.usableCapabilities.isEmpty())
    }

    @Test
    fun `adapter connect never fakes success`() = runBlocking {
        val adapter = adapterFor(null, mapOf("google_a" to "tok"))
        assertTrue(adapter.connect() is MoResult.Failure)
    }

    @Test
    fun `adapter disconnect clears only that account`() = runBlocking {
        val dir = Files.createTempDirectory("tasks-test").toFile()
        val adapter = adapterFor(null, mapOf("google_a" to "tok"), dir)
        val store = TaskExecutionStore(dir)
        store.recordExecution("google_a", TaskExecutionRecord("k", "l", "t1", "key", "T", 1L))
        store.recordExecution("google_b", TaskExecutionRecord("k", "l", "t2", "key", "T", 1L))
        assertTrue(adapter.disconnect("google_a") is MoResult.Success)
        assertNull(store.findExecution("google_a", "k"))
        assertNotNull(store.findExecution("google_b", "k"))
        // Idempotent.
        assertTrue(adapter.disconnect("google_a") is MoResult.Success)
        assertTrue(adapter.disconnect(null) is MoResult.Success)
    }

    // ---------- Helpers ----------

    private class FakeTasksServer {
        data class Recorded(
            val method: String,
            val path: String,
            val authHeader: String?,
            val methodOverride: String?,
            val body: String,
        )

        val recorded = mutableListOf<Recorded>()
        val routes = mutableMapOf<String, () -> TransportResponse>()
        /** When true, the transport throws IOException (network failure). */
        var failWithNetworkError = false

        val transport: HttpTransport = HttpTransport { method, url, headers, body ->
            val path = "/" + url.substringAfter("://").substringAfter("/").substringBefore("?")
            val bodyText = body ?: ""
            recorded.add(
                Recorded(
                    method,
                    path,
                    headers["Authorization"],
                    headers["X-HTTP-Method-Override"],
                    bodyText,
                ),
            )
            if (failWithNetworkError) throw IOException("simulated network failure")
            val handler = routes["$method $path"]
            handler?.invoke()
                ?: TransportResponse(404, """{"error":{"message":"no route"}}""")
        }

        fun baseUrl(): String = "http://fake.tasks"

        fun stop() {}
    }

    private class ConnectedTasksAdapter(private val connected: Boolean) : IntegrationAdapter {
        override val id: IntegrationId = IntegrationId.TASKS
        override val displayName: String = "Google Tasks"
        override val provider: String = "Google"
        override val declaredCapabilities: Set<IntegrationCapability> = setOf(
            IntegrationCapability.READ_TASK_LISTS,
            IntegrationCapability.CREATE_TASK,
        )
        override val requiredPermissions: List<PermissionDescription> = emptyList()

        private fun snap(accountId: String?) = IntegrationSnapshot(
            id = id,
            displayName = displayName,
            provider = provider,
            accountId = accountId,
            accountEmail = "a@example.com",
            status = if (connected) IntegrationStatus.CONNECTED else IntegrationStatus.DISCONNECTED,
            statusReason = if (connected) "ok" else "not connected",
            declaredCapabilities = declaredCapabilities,
            requiredPermissions = requiredPermissions,
            lastCheckedEpochMs = 0L,
        )

        override suspend fun snapshot(accountId: String?): IntegrationSnapshot = snap(accountId)
        override suspend fun connect(): MoResult<Unit> = MoResult.Success(Unit)
        override suspend fun disconnect(accountId: String?): MoResult<Unit> = MoResult.Success(Unit)
        override suspend fun refresh(accountId: String?): IntegrationSnapshot = snap(accountId)
    }

    private class FakeIntegrationStates : IntegrationStateRepository {
        override suspend fun upsert(record: IntegrationStateRecord) = Unit
        override suspend fun getForAccount(accountId: String?): List<IntegrationStateRecord> =
            emptyList()
        override suspend fun clear(integrationId: String, accountId: String?) = Unit
        override suspend fun clearForAccount(accountId: String?) = Unit
    }

    private fun managerWith(connected: Boolean): IntegrationManager {
        // Note: plain Dispatchers (not a TestDispatcher) — these tests use
        // runBlocking, so a StandardTestDispatcher would never advance and
        // withContext(io) would hang.
        return IntegrationManager(
            adapters = listOf(ConnectedTasksAdapter(connected)),
            states = FakeIntegrationStates(),
            dispatchers = AppDispatchers(),
        )
    }


}
