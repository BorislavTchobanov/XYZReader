package com.example.xyzreader.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.databinding.FragmentArticleDetailBinding;
import com.example.xyzreader.databinding.ListItemDateBinding;
import com.example.xyzreader.databinding.ListItemTextBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private FragmentArticleDetailBinding mBinding;
    private Cursor mCursor;
    private long mItemId;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        assert getArguments() != null;
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentArticleDetailBinding.inflate(inflater, container, false);

        bindViews();
        return mBinding.getRoot();
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mCursor == null)
            return;

        final List<String> items = new ArrayList<>();
        final Date publishedDate = parsePublishedDate();

        String date;
        if (!publishedDate.before(START_OF_EPOCH.getTime())) {
            date = DateUtils.getRelativeTimeSpanString(
                    publishedDate.getTime(),
                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL).toString()
                    + " by <strong>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</strong>";

        } else {
            // If date is before 1902, just show the string
            date = outputFormat.format(publishedDate) + " by <strong>"
                    + mCursor.getString(ArticleLoader.Query.AUTHOR)
                    + "</strong>";

        }
        items.add(date);

        for (String s : mCursor.getString(ArticleLoader.Query.BODY).split("(\\r\\n|\\n)")) {
            String trimmed = s.trim();
            if (!TextUtils.isEmpty(trimmed))
                items.add(s);
        }

        final Adapter adapter = new Adapter(items);
        mBinding.list.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false));
        mBinding.list.setAdapter(adapter);
        mBinding.list.setHasFixedSize(true);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int DATE_VIEW_TYPE = 0;
        private static final int TEXT_VIEW_TYPE = 1;

        private List<String> items;

        Adapter(List<String> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return DATE_VIEW_TYPE;
            return TEXT_VIEW_TYPE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            RecyclerView.ViewHolder viewHolder;

            switch (viewType) {
                case DATE_VIEW_TYPE: {
                    final ListItemDateBinding binding =
                            ListItemDateBinding.inflate(layoutInflater, parent, false);

                    viewHolder = new DateViewHolder(binding);
                }
                break;

                case TEXT_VIEW_TYPE:
                default: {
                    final ListItemTextBinding binding =
                            ListItemTextBinding.inflate(layoutInflater, parent, false);

                    viewHolder = new TextViewHolder(binding);
                }
                break;
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (getItemViewType(position)) {
                case DATE_VIEW_TYPE:
                    ((DateViewHolder) holder).bind(items.get(position));
                    break;
                case TEXT_VIEW_TYPE:
                    ((TextViewHolder) holder).bind(items.get(position));
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    public static class DateViewHolder extends RecyclerView.ViewHolder {
        final ListItemDateBinding binding;

        DateViewHolder(ListItemDateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String date) {
            this.binding.date.setText(Html.fromHtml(date), TextView.BufferType.SPANNABLE);
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        final ListItemTextBinding binding;

        TextViewHolder(ListItemTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String text) {
            this.binding.text.setText(Html.fromHtml(text), TextView.BufferType.SPANNABLE);
        }
    }
}
