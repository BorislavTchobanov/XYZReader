package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.databinding.ActivityArticleDetailBinding;

import java.util.Objects;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private ActivityArticleDetailBinding binding;
    private Cursor mCursor;
    private long mStartId;

    private ArticlePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_article_detail);

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.collapsingToolbarLayout.setTitleEnabled(false);

        getSupportLoaderManager().initLoader(0, null, this);

        pagerAdapter = new ArticlePagerAdapter(getSupportFragmentManager());
        binding.pager.setAdapter(pagerAdapter);
        binding.pager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        binding.pager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        binding.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                setCurrentItemToolbar();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        final AppCompatActivity launchingActivity = this;
        binding.shareFab.setOnClickListener(view -> {
            if (mCursor == null)
                return;

            startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(launchingActivity)
                    .setType("text/plain")
                    .setText(mCursor.getString(ArticleLoader.Query.TITLE))
                    .getIntent(), getString(R.string.action_share)));
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        pagerAdapter.notifyDataSetChanged();

        if (mStartId > 0) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    final int position = mCursor.getPosition();
                    binding.pager.setCurrentItem(position, false);
                    setCurrentItemToolbar();
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    public void setCurrentItemToolbar() {
        final String title = mCursor.getString(ArticleLoader.Query.TITLE);
        setTitle(title);

        GlideApp
                .with(getApplicationContext())
                .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Bitmap bitmap = ((BitmapDrawable) resource.getCurrent()).getBitmap();
                        changeToolbarColors(bitmap);
                        return false;
                    }
                })
                .into(binding.toolbarImage);
        binding.toolbarImage.setContentDescription(title);
    }

    private void changeToolbarColors(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            int defaultScrimColor = ContextCompat.getColor(this, R.color.colorPrimaryDark);
            int scrimColor = palette.getDarkMutedColor(defaultScrimColor);
            if (binding.collapsingToolbarLayout != null) {
                binding.collapsingToolbarLayout.setContentScrimColor(scrimColor);
                binding.collapsingToolbarLayout.setStatusBarScrimColor(scrimColor);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        pagerAdapter.notifyDataSetChanged();
    }

    private class ArticlePagerAdapter extends FragmentStatePagerAdapter {
        ArticlePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
