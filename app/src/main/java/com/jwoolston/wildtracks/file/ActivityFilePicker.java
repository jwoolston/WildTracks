package com.jwoolston.wildtracks.file;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jwoolston.wildtracks.R;
import com.nononsenseapps.filepicker.AbstractFilePickerActivity;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerFragment;

import java.io.File;

/**
 * @author Jared Woolston (jwoolston@idealcorp.com)
 */
public class ActivityFilePicker extends AbstractFilePickerActivity<File> {

    public static final String EXTRA_FILTER_EXTENSION = "ActivityFilePicker.EXTRA_FILTER_EXTENSION";

    private String filterPath;

    public ActivityFilePicker() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        filterPath = getIntent().getExtras().getString(EXTRA_FILTER_EXTENSION);
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected AbstractFilePickerFragment<File> getFragment(String s, int i, boolean b, boolean b1) {
        final StandaloneFilePickerFragment fragment = new StandaloneFilePickerFragment();
        fragment.setArgs(startPath, mode, allowMultiple, allowCreateDir, filterPath);
        return fragment;
    }

    public class StandaloneFilePickerFragment extends FilePickerFragment {

        protected Toolbar mToolbar;

        private String mFilterPath;

        public void setArgs(String startPath, int mode, boolean allowMultiple, boolean allowDirCreate, String path) {
            super.setArgs(startPath, mode, allowMultiple, allowDirCreate);
            mFilterPath = path;
        }

        @Override
        protected void setupToolbar(Toolbar toolbar) {
            // Prevent it from being set as main toolbar by NOT calling super.setupToolbar().
            mToolbar = toolbar;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Populate the toolbar with the menu items instead of the action bar.
            mToolbar.inflateMenu(R.menu.picker_actions);

            // Set a menu listener on the toolbar with calls the regular onOptionsItemSelected method.
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });

            // This is usually handled in onCreateOptions so do it here instead.
            final MenuItem item = mToolbar.getMenu().findItem(com.nononsenseapps.filepicker.R.id.nnf_action_createdir);
            item.setVisible(allowCreateDir);
        }

        /**
         * @param file
         *
         * @return The file extension. If file has no extension, it returns null.
         */
        private String getExtension(@NonNull File file) {
            final String path = file.getPath();
            int i = path.lastIndexOf(".");
            if (i < 0) {
                return null;
            } else {
                return path.substring(i);
            }
        }

        @Override
        protected boolean isItemVisible(final File file) {
            if (!isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
                return mFilterPath == null || mFilterPath.equalsIgnoreCase(getExtension(file));
            }
            return isDir(file);
        }
    }
}
