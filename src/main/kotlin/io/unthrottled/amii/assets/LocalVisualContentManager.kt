package io.unthrottled.amii.assets

import io.unthrottled.amii.config.Config
import io.unthrottled.amii.tools.AssetTools.calculateMD5Hash
import io.unthrottled.amii.tools.Logging
import io.unthrottled.amii.tools.logger
import io.unthrottled.amii.tools.runSafelyWithResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.stream.Stream

// todo: don't read the entire directory each time.
object LocalVisualContentManager : Logging {

  fun supplyAllVisualAssetDefinitionsFromDefaultDirectory(): Set<VisualAssetRepresentation> {
    return readLocalAssetDirectory(Config.instance.customAssetsPath)
  }

  private var ledger = LocalVisualAssetStorageService.getInitialItem()

  // todo: let user modified list stay global & only supply assets in directory.
  fun supplyUserModifiedVisualRepresentations(): Set<VisualAssetRepresentation> {
    return ledger.savedVisualAssets.values
      .filter { rep ->
        rep.lewd != true ||
          Config.instance.allowLewds
      }
      .toSet()
  }

  fun updateRepresentation(visualAssetRepresentation: VisualAssetRepresentation) {
    val newMap = ledger.savedVisualAssets.toMutableMap()
    newMap[visualAssetRepresentation.id] = visualAssetRepresentation
    ledger = ledger.copy(savedVisualAssets = newMap)
    LocalVisualAssetStorageService.persistLedger(ledger)
  }

  fun updateRepresentations(visualAssetRepresentations: List<VisualAssetRepresentation>) {
    val newMap = ledger.savedVisualAssets.toMutableMap()
    visualAssetRepresentations.forEach { visualAssetRepresentation ->
      newMap[visualAssetRepresentation.id] = visualAssetRepresentation
    }
    ledger = ledger.copy(savedVisualAssets = newMap)
    LocalVisualAssetStorageService.persistLedger(ledger)
  }

  @JvmStatic
  fun supplyAllVisualAssetDefinitionsFromWorkingDirectory(
    workingDirectory: String
  ): Set<VisualAssetRepresentation> {
    return readLocalAssetDirectory(workingDirectory)
  }

  private fun readLocalAssetDirectory(workingDirectory: String): Set<VisualAssetRepresentation> {
    if (workingDirectory.isEmpty() ||
      Files.exists(Paths.get(workingDirectory)).not()
    ) {
      return emptySet()
    }

    return runSafelyWithResult({
      walkDirectoryForAssets(workingDirectory)
        .map { path ->
          val id = calculateMD5Hash(path)
          val savedAsset = ledger.savedVisualAssets[id]
          savedAsset?.duplicateWithNewPath(path.toUri().toString())
              ?: VisualAssetRepresentation(
                id,
                path.toUri().toString(),
                "", ArrayList(), ArrayList(),
                "",
                false
              )
        }
        .collect(Collectors.toSet())
    }) {
      this.logger().warn("Unable to walk custom working directory for raisins.", it)
      emptySet()
    }
  }

  @JvmStatic
  fun walkDirectoryForAssets(workingDirectory: String): Stream<Path> = Files.walk(
    Paths.get(workingDirectory)
  )
    .filter { path: Path ->
      Files.isReadable(
        path
      )
    }
    .filter { path: Path ->
      Files.isRegularFile(
        path
      )
    }
    .filter { path ->
      path.fileName.toString().endsWith(".gif")
    }
}
