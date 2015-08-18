package de.droidwiki.onboarding;

public interface OnboardingStateMachine {
    boolean isTocTutorialEnabled();
    void setTocTutorial();
    boolean isSelectTextTutorialEnabled();
    void setSelectTextTutorial();
    boolean isShareTutorialEnabled();
    void setShareTutorial();
}