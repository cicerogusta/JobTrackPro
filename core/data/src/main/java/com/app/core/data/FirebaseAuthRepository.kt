package com.app.core.data

import com.app.core.model.*
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(private val firebase: FirebaseProvider) : AuthRepository {
    private val mutableSession = MutableStateFlow<Session>(Session.Loading)
    override val session = mutableSession.asStateFlow()
    init {
        if (!firebase.configured) mutableSession.value = Session.Unconfigured
        else firebase.auth.addAuthStateListener { auth ->
            mutableSession.value = auth.currentUser?.let {
                Session.Authenticated(UserProfile(it.uid, it.email.orEmpty(), it.displayName.orEmpty()))
            } ?: Session.SignedOut
        }
    }
    override suspend fun signIn(email: String, password: String) = dataOperation {
        AuthValidator.validate(email, password, registering = false)
        firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }
    override suspend fun register(email: String, password: String, name: String) = dataOperation {
        AuthValidator.validate(email, password, registering = true, name)
        val user = firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: throw AppException(Failure.AUTHENTICATION)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        mutableSession.value = Session.Authenticated(UserProfile(user.uid, user.email.orEmpty(), name.trim()))
    }
    override suspend fun resetPassword(email: String) = dataOperation {
        if (!AuthValidator.validEmail(email)) throw AppException(Failure.VALIDATION)
        firebase.auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override fun signOut() { if (firebase.configured) firebase.auth.signOut() }
    override fun requireUserId(): String =
        (session.value as? Session.Authenticated)?.user?.id ?: throw AppException(Failure.AUTHENTICATION)
}
