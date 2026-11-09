/*
 * Odak: scroll dürtüsünü Anki çalışmasına çeviren katman.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.ichi2.anki.odak

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.Reviewer
import com.ichi2.anki.odak.data.Repo
import com.ichi2.anki.odak.ui.OdakTheme
import com.ichi2.anki.odak.ui.SplashOverlay
import com.ichi2.anki.odak.ui.StopActivity
import com.ichi2.anki.odak.work.BatchWorker
import com.ichi2.anki.odak.work.Notifs
import com.ichi2.anki.odak.work.ReminderWorker

/**
 * AnkiDroid ile Odak arasındaki tüm bağlantı noktaları burada toplanır;
 * AnkiDroid'in kendi dosyalarında yalnızca birkaç satırlık çağrı vardır.
 */
object OdakHooks {
    /** Reviewer bu kadar kart cevaplanınca kapanır ve Odak'ın "bitti" ekranına döner. */
    const val EXTRA_GATE_LIMIT = "odak_gate_limit"

    fun init(app: Application) {
        try {
            Repo.init(app)
            Notifs.createChannel(app)
            if (Repo.current.settings.onboarded) ReminderWorker.schedule(app)
            BatchWorker.schedule(app)
            app.registerActivityLifecycleCallbacks(SplashCallbacks)
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Odak başlatılamadı")
        }
    }

    // ---------- Durdurma ekranı → Anki çalışma ekranı ----------

    /** Vazgeçince açılan Anki çalışma ekranı: seçili desteden [Settings.gateCards] kart. */
    fun gateReviewerIntent(context: Context): Intent {
        val s = Repo.current.settings
        return Intent(context, Reviewer::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_GATE_LIMIT, s.gateCards)
            .apply { if (s.deckId != 0L) putExtra(Reviewer.EXTRA_DECK_ID, s.deckId) }
    }

    /** Ana sayfadaki "Şimdi çalış": sınırsız, normal Anki çalışması. */
    fun studyIntent(context: Context): Intent {
        val s = Repo.current.settings
        return Intent(context, Reviewer::class.java)
            .setAction(Intent.ACTION_VIEW)
            .apply { if (s.deckId != 0L) putExtra(Reviewer.EXTRA_DECK_ID, s.deckId) }
    }

    fun decksIntent(context: Context): Intent =
        Intent(context, DeckPicker::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** Reviewer'dan: her cevap Odak günlüğüne (değerlendirme sayısı, doğru oranı, ritim tiki). */
    fun onAnswered(correct: Boolean) {
        try {
            Repo.logReview(correct)
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Odak: cevap kaydedilemedi")
        }
    }

    /** Durdurma ekranından açılan çalışma bitti. */
    fun onGateFinished(activity: Activity, answered: Int, noDue: Boolean) {
        Repo.logSession(answered)
        activity.startActivity(StopActivity.doneIntent(activity, answered, noDue))
    }

    // ---------- Her açılışta yükleme ekranı ----------

    private object SplashCallbacks : Application.ActivityLifecycleCallbacks {
        private var started = 0
        private var changingConfig = false
        private var pending = false

        override fun onActivityStarted(activity: Activity) {
            if (started == 0 && !changingConfig) pending = true
            changingConfig = false
            started++
        }

        override fun onActivityStopped(activity: Activity) {
            started = (started - 1).coerceAtLeast(0)
            if (activity.isChangingConfigurations) changingConfig = true
        }

        override fun onActivityResumed(activity: Activity) {
            if (!pending) return
            pending = false
            // Dürtü anında gecikme olmasın: durdurma ekranı ve oradan açılan çalışma hariç.
            if (activity is StopActivity || activity.intent?.hasExtra(EXTRA_GATE_LIMIT) == true) return
            if (activity !is ComponentActivity) return
            attachSplash(activity)
        }

        private fun attachSplash(activity: ComponentActivity) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
            val view = ComposeView(activity)
            view.setContent {
                OdakTheme {
                    SplashOverlay(onFinished = { root.post { root.removeView(view) } })
                }
            }
            try {
                root.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Odak: açılış ekranı eklenemedi")
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
