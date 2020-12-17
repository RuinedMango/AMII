package io.unthrottled.amii.personality

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Consumer
import io.unthrottled.amii.Icons
import io.unthrottled.amii.config.Config
import io.unthrottled.amii.config.ConfigListener
import io.unthrottled.amii.config.ui.PluginSettingsUI
import io.unthrottled.amii.core.personality.emotions.EMOTION_TOPIC
import io.unthrottled.amii.core.personality.emotions.Mood
import io.unthrottled.amii.core.personality.emotions.MoodListener
import io.unthrottled.amii.tools.toOptional
import java.awt.event.MouseEvent
import java.util.Optional
import javax.swing.Icon

class MoodStatusBarWidget(private val project: Project) :
  StatusBarWidget,
  StatusBarWidget.IconPresentation {
  companion object {
    private const val ID = "io.unthrottled.amii.personality.MoodStatusBarWidget"
  }

  private val connect = ApplicationManager.getApplication().messageBus.connect()

  private lateinit var seenMood: Mood

  private val currentMood: Optional<Mood>
    get() = if (this::seenMood.isInitialized) seenMood.toOptional()
    else Optional.empty()

  init {
    connect.subscribe(
      LafManagerListener.TOPIC,
      LafManagerListener {
        updateWidget()
      }
    )
    connect.subscribe(
      ConfigListener.CONFIG_TOPIC,
      ConfigListener { updateWidget() }
    )
    connect.subscribe(
      EMOTION_TOPIC,
      object : MoodListener {
        override fun onDerivedMood(currentMood: Mood) {
          seenMood = currentMood
          updateWidget()
        }
      }
    )
    ApplicationManager.getApplication().messageBus.syncPublisher(EMOTION_TOPIC).onRequestMood()
    StartupManager.getInstance(project).runWhenProjectIsInitialized { updateWidget() }
  }

  private fun updateWidget() {
    WindowManager.getInstance().getStatusBar(project).toOptional()
      .ifPresent {
        it.updateWidget(ID)
      }
  }

  override fun getTooltipText(): String = currentMood
    .filter { Config.instance.showMood }
    .map { it.toString().toLowerCase() }
    .map { "MIKU is $it." }
    .orElse("")

  override fun ID(): String = ID

  override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

  override fun install(statusBar: StatusBar) {
    statusBar.updateWidget(ID)
  }

  override fun dispose() {
  }

  override fun getIcon(): Icon? =
    currentMood
      .filter { Config.instance.showMood }
      .map { getEmoji(it) }
      .orElse(null)

  private fun getEmoji(mood: Mood): Icon {
    return when (mood) {
      Mood.ENRAGED -> Icons.E1F92C
      Mood.FRUSTRATED -> Icons.E1F620
      Mood.AGITATED -> Icons.E1F612
      Mood.HAPPY -> Icons.E1F60A
      Mood.RELIEVED -> Icons.E1F60C
      Mood.EXCITED -> Icons.E1F973
      Mood.SMUG -> Icons.E1F60F
      Mood.SHOCKED -> Icons.E1F632
      Mood.DISAPPOINTED -> Icons.E1F62D
      else -> Icons.E1F642
    }
  }

  override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
    ShowSettingsUtil.getInstance()
      .showSettingsDialog(project, PluginSettingsUI::class.java)
  }
}