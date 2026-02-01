package cn.mklaus.sqlagent.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * Action to manually show the setup wizard from Tools menu
 */
public class ShowWizardAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        FirstRunWizard wizard = new FirstRunWizard();
        wizard.show();
    }
}
