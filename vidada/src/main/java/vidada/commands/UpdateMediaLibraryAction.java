package vidada.commands;

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import vidada.views.ImageResources;
import vidada.viewsFX.dialoges.SynchronizeDialog;

/**
 * Updates all media libraries
 * @author IsNull
 *
 */
@SuppressWarnings("serial")
public class UpdateMediaLibraryAction extends AbstractAction {

	private final Window owner;

	public UpdateMediaLibraryAction(Window owner){
		super("", ImageResources.UPDATELIB_ICON_32);
		this.putValue(Action.SHORT_DESCRIPTION, "Update Library");

		this.owner = owner;
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		SynchronizeDialog scanUpdateDialog = new SynchronizeDialog(null);
		scanUpdateDialog.show();
	}

}
