package io.unthrottled.amii.config.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import io.unthrottled.amii.assets.AudibleContent;
import io.unthrottled.amii.assets.MemeAsset;
import io.unthrottled.amii.assets.VisualAssetEntity;
import io.unthrottled.amii.assets.VisualMemeContent;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class CustomMemePanel {
  private JPanel memeSettings;
  private JButton testMeme;
  private JPanel visualAssetDisplay;
  private JPanel rootPane;
  private JBLabel memeDisplay;
  private JPanel categoriesPanel;
  private JPanel audioAssetPath;

  private String audioAssetURL = null;

  public CustomMemePanel(
    Consumer<MemeAsset> onTest,
    VisualAssetEntity visualAssetEntity
  ) {
    String assetUri = visualAssetEntity.getPath();
    @Language("HTML") String meme = "<html><img src=\""+ assetUri + "\" /></html>";
    memeDisplay.setText(meme);

    testMeme.addActionListener(a ->
      onTest.accept(
        new MemeAsset(
          new VisualMemeContent(
            "aoeu",
            URI.create(assetUri),
            "",
            null
          ),
          this.audioAssetURL == null ? null :
            new AudibleContent(
              Paths.get(this.audioAssetURL).toUri()
            )
        )
      ));
  }

  public Optional<MemeAsset> getMemeAsset() {
    return Optional.empty();
  }

  public JPanel getComponent() {
    return rootPane;
  }

  private void createUIComponents() {
    categoriesPanel = MemeCategoriesPanel.createComponent();

    TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton();
    textFieldWithBrowseButton.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(ExecutionBundle.message("select.working.directory.message"), null,
      textFieldWithBrowseButton,
      Arrays.stream(ProjectManager.getInstance().getOpenProjects()).findFirst().orElse(
        ProjectManager.getInstance().getDefaultProject()
      ),
      FileChooserDescriptorFactory.createSingleFileDescriptor(),
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT) {
      @Override
      protected void onFileChosen(@NotNull VirtualFile chosenFile) {
        super.onFileChosen(chosenFile);
        audioAssetURL = chosenFile.getPath();
      }
    });
    this.audioAssetPath = LabeledComponent.create(textFieldWithBrowseButton,
      ExecutionBundle.message("run.configuration.working.directory.label"));
  }
}
