package com.poupa.vinylmusicplayer.adapter;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.adapter.base.AbsMultiSelectAdapter;
import com.poupa.vinylmusicplayer.adapter.base.MediaEntryViewHolder;
import com.poupa.vinylmusicplayer.databinding.ItemListBinding;
import com.poupa.vinylmusicplayer.glide.GlideApp;
import com.poupa.vinylmusicplayer.glide.audiocover.AudioFileCover;
import com.poupa.vinylmusicplayer.interfaces.CabHolder;
import com.poupa.vinylmusicplayer.util.ImageTheme.ThemeStyleUtil;
import com.poupa.vinylmusicplayer.util.ImageUtil;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongFileAdapter extends AbsMultiSelectAdapter<SongFileAdapter.ViewHolder, File> implements FastScrollRecyclerView.SectionedAdapter {

    private static final int FILE = 0;
    private static final int FOLDER = 1;

    private final AppCompatActivity activity;
    private List<File> dataSet;
    @Nullable
    private final Callbacks callbacks;

    public SongFileAdapter(@NonNull AppCompatActivity activity, @NonNull List<File> songFiles,
                           @Nullable Callbacks callback, @Nullable CabHolder cabHolder) {
        super(activity, cabHolder, R.menu.menu_media_selection);
        this.activity = activity;
        this.dataSet = songFiles;
        this.callbacks = callback;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        return dataSet.get(position).isDirectory() ? FOLDER : FILE;
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).hashCode();
    }

    public void swapDataSet(@NonNull List<File> songFiles) {
        this.dataSet = songFiles;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListBinding binding = ItemListBinding.inflate(LayoutInflater.from(activity), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        final File file = dataSet.get(index);

        holder.itemView.setActivated(isChecked(file));

        if (holder.getAdapterPosition() == getItemCount() - 1) {
            if (holder.shortSeparator != null) {
                holder.shortSeparator.setVisibility(View.GONE);
            }
        } else {
            if (holder.shortSeparator != null) {
                holder.shortSeparator.setVisibility(ThemeStyleUtil.getInstance().getShortSeparatorVisibilityState());
            }
        }

        if (holder.title != null) {
            holder.title.setText(getFileTitle(file));
        }
        if (holder.text != null) {
            if (holder.getItemViewType() == FILE) {
                holder.text.setText(getFileText(file));
            } else {
                holder.text.setVisibility(View.GONE);
            }
        }

        if (holder.image != null) {
            loadFileImage(file, holder);
        }
    }

    protected String getFileTitle(File file) {
        return file.getName();
    }

    protected String getFileText(File file) {
        return file.isDirectory() ? null : readableFileSize(file.length());
    }

    @SuppressWarnings("ConstantConditions")
    protected void loadFileImage(File file, final ViewHolder holder) {
        final int iconColor = ATHUtil.resolveColor(activity, R.attr.iconColor);
        if (file.isDirectory()) {
            holder.image.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            holder.image.setImageResource(R.drawable.ic_folder_white_24dp);
        } else {
            Drawable error = ImageUtil.getTintedVectorDrawable(activity, R.drawable.ic_file_music_white_24dp, iconColor);
            GlideApp.with(activity)
                    .load(new AudioFileCover(file.getPath()))
                    .transition(GenericTransitionOptions.with(android.R.anim.fade_in))
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .error(error)
                            .placeholder(error)
                            .signature(new MediaStoreSignature("", file.lastModified(), 0)))
                    .into(holder.image);
        }
    }

    public static String readableFileSize(long size) {
        if (size <= 0) return size + " B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    protected File getIdentifier(int position) {
        return dataSet.get(position);
    }

    @Override
    protected void onMultipleItemAction(MenuItem menuItem, ArrayList<File> selection) {
        if (callbacks == null) return;
        callbacks.onMultipleItemAction(menuItem, selection);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        return String.valueOf(dataSet.get(position).getName().charAt(0)).toUpperCase();
    }

    public class ViewHolder extends MediaEntryViewHolder {
        public ViewHolder(@NonNull ItemListBinding binding) {
            super(binding);

            View itemView = binding.getRoot();
            ThemeStyleUtil.getInstance().setHeightListItem(itemView, activity.getResources().getDisplayMetrics().density);
            imageBorderTheme.setRadius(ThemeStyleUtil.getInstance().getAlbumRadiusImage(activity));

            if (menu != null && callbacks != null) {
                menu.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (isPositionInRange(position)) {
                        callbacks.onFileMenuClicked(dataSet.get(position), v);
                    }
                });
            }
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (isPositionInRange(position)) {
                if (isInQuickSelectMode()) {
                    toggleChecked(position);
                } else {
                    if (callbacks != null) {
                        callbacks.onFileSelected(dataSet.get(position));
                    }
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getAdapterPosition();
            return isPositionInRange(position) && toggleChecked(position);
        }

        private boolean isPositionInRange(int position) {
            return position >= 0 && position < dataSet.size();
        }
    }

    public interface Callbacks {
        void onFileSelected(File file);

        void onFileMenuClicked(File file, View view);

        void onMultipleItemAction(MenuItem item, ArrayList<File> files);
    }
}
