package com.blazeroni.reddit.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.blazeroni.reddit.widget.R;

/**
 * The ImageListPreference class responsible for displaying an image for each
 * item within the list.
 *
 * @author Casper Wakkers
 * @author Dean Allen
 */
public class ImageListPreference extends ListPreference {
    private CharSequence[] imagePaths;
    private int[] resourceIds = null;

    /**
     * Constructor of the ImageListPreference. Initializes the custom images.
     * @param context application context.
     * @param attrs custom xml attributes.
     */
    public ImageListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImageListPreference);

        this.imagePaths = a.getTextArray(R.styleable.ImageListPreference_entryImages);

        a.recycle();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        ensureResources();

        int index = findIndexOfValue(getValue());

        ListAdapter listAdapter = new ImageArrayAdapter(getContext(),
            R.layout.pref_image_list_item, getEntries(), this.resourceIds, index);

        // Order matters.
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    private void ensureResources() {
        if (this.resourceIds == null) {
            Context context = getContext();
            String packageName = context.getPackageName();
            Resources resources = context.getResources();

            this.resourceIds = new int[this.imagePaths.length];
            for (int i = 0; i < this.imagePaths.length; i++) {
                CharSequence name = this.imagePaths[i];
                name = name.subSequence(4, name.length() - 4); // remove "res/" and ".xml"
                this.resourceIds[i] = resources.getIdentifier(name.toString(), null, packageName);
            }
            this.imagePaths = null;
        }
    }

    static class ImageArrayAdapter extends ArrayAdapter<CharSequence> {
        private int index = 0;
        private int[] resourceIds = null;

        /**
         * ImageArrayAdapter constructor.
         * @param context the context.
         * @param textViewResourceId resource id of the text view.
         * @param objects to be displayed.
         * @param ids resource id of the images to be displayed.
         * @param i index of the previous selected item.
         */
        public ImageArrayAdapter(Context context, int textViewResourceId,
                CharSequence[] objects, int[] ids, int i) {
            super(context, textViewResourceId, objects);

            this.index = i;
            this.resourceIds = ids;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View row = inflater.inflate(R.layout.pref_image_list_item, parent, false);

            ImageView imageView = (ImageView) row.findViewById(R.id.image);
            imageView.setImageResource(this.resourceIds[position]);

            CheckedTextView checkedTextView = (CheckedTextView)row.findViewById(R.id.check);

            checkedTextView.setText(getItem(position));

            if (position == this.index) {
                checkedTextView.setChecked(true);
            }

            return row;
        }
    }
}
