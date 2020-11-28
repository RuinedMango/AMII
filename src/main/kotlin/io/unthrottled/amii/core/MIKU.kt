package io.unthrottled.amii.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import io.unthrottled.amii.config.Config
import io.unthrottled.amii.core.personality.IdlePersonalityCore
import io.unthrottled.amii.core.personality.ResetCore
import io.unthrottled.amii.core.personality.TaskPersonalityCore
import io.unthrottled.amii.core.personality.emotions.EMOTION_TOPIC
import io.unthrottled.amii.core.personality.emotions.EmotionCore
import io.unthrottled.amii.core.personality.emotions.EmotionalMutationAction
import io.unthrottled.amii.core.personality.emotions.EmotionalMutationActionListener
import io.unthrottled.amii.core.personality.emotions.EmotionalMutationType
import io.unthrottled.amii.core.personality.emotions.Mood
import io.unthrottled.amii.events.UserEvent
import io.unthrottled.amii.events.UserEventListener
import io.unthrottled.amii.events.UserEvents
import io.unthrottled.amii.memes.MemeFactory
import io.unthrottled.amii.tools.AlarmDebouncer
import io.unthrottled.amii.tools.Logging
import io.unthrottled.amii.tools.logger
import java.util.Optional

// Meme Inference Knowledge Unit
class MIKU : UserEventListener, EmotionalMutationActionListener, Disposable, Logging {

  companion object {
    private const val DEBOUNCE_INTERVAL = 80
  }

  private var emotionCore = EmotionCore(Config.instance)
  private val taskPersonalityCore = TaskPersonalityCore()
  private val idlePersonalityCore = IdlePersonalityCore()
  private val resetCore = ResetCore()
  private val singleEventDebouncer = AlarmDebouncer<UserEvent>(DEBOUNCE_INTERVAL, this)
  private val idleEventDebouncer = AlarmDebouncer<UserEvent>(DEBOUNCE_INTERVAL, this)

  override fun onDispatch(userEvent: UserEvent) {
    logger().warn("Seen user event $userEvent")
    when (userEvent.type) {
      UserEvents.IDLE ->
        idleEventDebouncer.debounceAndBuffer(userEvent) {
          consumeEvents(it)
        }
      else -> singleEventDebouncer.debounce {
        consumeEvent(userEvent)
      }
    }

    // todo: remove when figured out meme display API
    when (userEvent.eventName) {
      "Show Random" -> MemeFactory.createMemeDisplay(userEvent.project)
      else -> Optional.empty()
    }.ifPresent {
      it.display()
    }
  }

  private fun consumeEvents(bufferedUserEvents: List<UserEvent>) {
    val emotionalState = emotionCore.deriveMood(bufferedUserEvents.first())
    bufferedUserEvents.forEach { userEvent -> reactToEvent(userEvent, emotionalState) }
  }

  private fun consumeEvent(userEvent: UserEvent) {
    val currentMood = emotionCore.deriveMood(userEvent)
    reactToEvent(userEvent, currentMood)
    publishMood(currentMood)
  }

  override fun onAction(emotionalMutationAction: EmotionalMutationAction) {
    val mutatedMood = emotionCore.mutateMood(emotionalMutationAction)
    reactToMutation(emotionalMutationAction)
    publishMood(mutatedMood)
  }

  private fun reactToMutation(
    emotionalMutationAction: EmotionalMutationAction
  ) {
    if (emotionalMutationAction.type == EmotionalMutationType.RESET) {
      resetCore.processMutationEvent(emotionalMutationAction)
    }
  }

  private fun publishMood(currentMood: Mood) {
    ApplicationManager.getApplication().messageBus
      .syncPublisher(EMOTION_TOPIC)
      .onDerivedMood(currentMood)
  }

  private fun reactToEvent(userEvent: UserEvent, emotionalState: Mood) {
    when (userEvent.type) {
      UserEvents.TEST,
      UserEvents.TASK,
      UserEvents.PROCESS -> taskPersonalityCore.processUserEvent(userEvent, emotionalState)
      UserEvents.IDLE -> idlePersonalityCore.processUserEvent(userEvent, emotionalState)
      else -> {
      }
    }
  }

  override fun dispose() {
  }
}