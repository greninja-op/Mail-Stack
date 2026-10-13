package com.greninjaop.mailorganizer.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greninjaop.mailorganizer.di.AppContainer

/** Factory for the Phase 6 mailbox screen. */
class MailViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MailViewModel(
            accounts = container.accountRepository,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            classifyMailbox = container.classifyMailboxUseCase,
            companyIntelligence = container.companyIntelligenceUseCase,
            prioritizeMailbox = container.prioritizeMailboxUseCase,
            extractMailbox = container.extractMailboxUseCase,
            generateActions = container.generateActionsUseCase,
            syncCoordinator = container.syncCoordinator,
            connectivity = container.connectivityObserver,
            dispatchers = container.dispatchers,
            samplePolicy = container.sampleDataPolicy,
            seeder = container.sampleMailboxSeeder,
            activeAccountPreferences = container.activeAccountPreferences,
            syncScheduler = container.syncScheduler,
            syncStateRepository = container.syncStateRepository,
        ) as T
    }
}

/** Factory for the Phase 6 thread screen (threadId comes from navigation). */
class ThreadViewModelFactory(
    private val container: AppContainer,
    private val threadId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThreadViewModel(
            threadId = threadId,
            mail = container.mailRepository,
            intelligence = container.intelligenceRepository,
            dispatchers = container.dispatchers,
            conversationIntelligence = container.conversationUseCase,
            aiFallback = container.aiFallbackUseCase,
            // Phase 22: Gmail write operations behind the confirmation
            // boundary. Built here (not in AppContainer) so the auth flow
            // wiring stays untouched.
            gmailWrite = com.greninjaop.mailorganizer.domain.write.GmailWriteUseCase(
                writeApi = container.gmailApiClient,
                mail = container.mailRepository,
                auth = container.authRepository,
                dispatchers = container.dispatchers,
            ),
            accounts = container.accountRepository,
            // Fetch-on-open for message bodies (2026-10-13).
            fetchBody = container.fetchMessageBodyUseCase,
        ) as T
    }
}
