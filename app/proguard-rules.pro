# ------------------------------------------------------------------
# Alpha Clone ProGuard / R8 rules
# ------------------------------------------------------------------
# These only take effect on release builds (isMinifyEnabled = true).
# Debug builds are never shrunk, so day-to-day development is unaffected.

# --- JGit ---
# JGit does a fair amount of reflection (transport plugin discovery,
# service loading) — keep it whole rather than risk stripping something
# it looks up dynamically at runtime.
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-dontwarn org.slf4j.**
-dontwarn com.jcraft.jsch.**
-dontwarn com.googlecode.javaewah.**

# --- SSH (Apache MINA sshd, via org.eclipse.jgit.ssh.apache) ---
# Same story as JGit itself: does service-loader discovery (cipher/kex/signature
# algorithm registration) and a fair amount of reflection for key format handling.
-keep class org.apache.sshd.** { *; }
-dontwarn org.apache.sshd.**
-dontwarn net.i2p.crypto.eddsa.**

# --- GPG commit signing (Bouncy Castle, via org.eclipse.jgit.gpg.bc) ---
# BouncyCastleGpgKeyLocator and the OpenPGP packet parser both do provider-style
# lookups; keep the crypto/openpgp packages whole rather than risk a stripped
# codec breaking key parsing at runtime in a way that's hard to reproduce in debug.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.eclipse.jgit.gpg.bc.** { *; }

# --- Room ---
# Room's generated code (schema classes, DAOs) is created at compile time
# and referenced via generated implementations; keep entities and DAOs
# intact so those generated classes still line up.
#
# NOTE: these paths were wrong before this pass (missing the app's actual
# package segment entirely, so they never matched any real class) — R8
# would have been free to strip Room's entities/DAOs/TokenCrypto on any
# release build, a latent bug that a debug build would never surface.
-keep class com.willykez.gitsync.data.db.entity.** { *; }
-keep interface com.willykez.gitsync.data.db.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Kotlin coroutines ---
# Coroutines use some reflection-adjacent internals (debug probes, service
# loading for the Main dispatcher) that shrinking can otherwise break.
-dontwarn kotlinx.coroutines.debug.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# --- Kotlin metadata / reflection ---
# Keeps enough metadata around for kotlin-reflect-adjacent library code
# (Room, etc.) that inspects Kotlin classes at runtime.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keep class kotlin.Metadata { *; }

# --- Jetpack Compose ---
# Compose Compiler-generated code is already shrink-safe by default via
# the AGP-provided consumer rules; nothing extra needed here, but keeping
# this section as a marker in case a future Compose library needs a rule.

# --- Android Keystore / crypto ---
# TokenCrypto uses javax.crypto + Android Keystore reflectively in some
# OEM implementations.
-dontwarn javax.crypto.**
-keep class com.willykez.gitsync.data.repository.TokenCrypto { *; }

# --- General ---
# Keep line numbers for readable stack traces from crash reports, but
# rename the source file attribute so it doesn't leak the original
# filename structure.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
