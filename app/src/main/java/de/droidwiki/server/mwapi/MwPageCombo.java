package de.droidwiki.server.mwapi;

import de.droidwiki.server.PageCombo;

/**
 * Combines MwPageLead and MwPageRemaining Gson POJOs for mobileview API.
 * In mobileview API the implementation is basically the same as MwPageLead.
 * The class name "Page" was already used, and is very entrenched in this code base.
 */
public class MwPageCombo extends MwPageLead implements PageCombo {
}