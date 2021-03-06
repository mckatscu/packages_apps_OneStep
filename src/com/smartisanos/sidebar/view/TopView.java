package com.smartisanos.sidebar.view;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.SidebarController;
import com.smartisanos.sidebar.SidebarMode;
import com.smartisanos.sidebar.SidebarStatus;
import com.smartisanos.sidebar.util.BitmapUtils;
import com.smartisanos.sidebar.util.BookmarkManager;
import com.smartisanos.sidebar.util.Constants;
import com.smartisanos.sidebar.util.FileInfo;
import com.smartisanos.sidebar.util.ImageInfo;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.RecentClipManager;
import com.smartisanos.sidebar.util.RecentFileManager;
import com.smartisanos.sidebar.util.RecentPhotoManager;
import com.smartisanos.sidebar.util.Utils;
import com.smartisanos.sidebar.util.anim.Anim;
import com.smartisanos.sidebar.util.anim.AnimListener;
import com.smartisanos.sidebar.util.anim.AnimStatusManager;
import com.smartisanos.sidebar.util.anim.AnimTimeLine;
import com.smartisanos.sidebar.util.anim.Vector3f;
import com.smartisanos.sidebar.view.ContentView.ContentType;

import android.content.Context;
import android.content.CopyHistoryItem;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class TopView extends FrameLayout {
    private static final LOG log = LOG.getInstance(TopView.class);

    private SidebarController mController;

    private DimSpaceView mDisableView;
    private DimSpaceView mLeft, mRight;
    private TopItemView mPhotos, mFile, mClipboard;

    private Map<ITopItem, ContentType> mViewToType;

    private RecentPhotoManager mPhotoManager;
    private RecentFileManager mFileManager;
    private RecentClipManager mClipManager;

    private int mTopbarPhotoIconContentSize ;
    private int mTopbarPhotoIconContentPaddingTop;
    private int mTopbarFileIconContentPaddingTop;

    private View mShadowLine;

    private Context mContext;

    public TopView(Context context) {
        this(context, null);
    }

    public TopView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TopView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        mController = SidebarController.getInstance(mContext);
        mTopbarPhotoIconContentSize = context.getResources().getDimensionPixelSize(R.dimen.topbar_photo_icon_content_size);
        mTopbarPhotoIconContentPaddingTop = context.getResources().getDimensionPixelSize(R.dimen.topbar_photo_icon_content_paddingTop);
        mTopbarFileIconContentPaddingTop = context.getResources().getDimensionPixelSize(R.dimen.topbar_file_icon_content_paddingTop);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDisableView = (DimSpaceView) findViewById(R.id.topbar_disable_view);
        mLeft = (DimSpaceView) findViewById(R.id.top_dim_view_left);
        mRight = (DimSpaceView) findViewById(R.id.top_dim_view_right);

        mPhotos = (TopItemView) findViewById(R.id.photo);
        mPhotos.setText(R.string.topbar_photo);
        mPhotos.setIconBackground(R.drawable.topbar_photo, R.drawable.topbar_photo_dim);

        mFile = (TopItemView) findViewById(R.id.file);
        mFile.setText(R.string.topbar_file);
        mFile.setIconBackground(R.drawable.topbar_file, R.drawable.topbar_file_dim);

        mClipboard = (TopItemView) findViewById(R.id.clipboard);
        mClipboard.setText(R.string.topbar_clipboard);
        mClipboard.setIconBackground(R.drawable.topbar_clipboard, R.drawable.topbar_clipboard_dim);

        mViewToType = new HashMap<ITopItem, ContentType>();
        mViewToType.put(mLeft, ContentType.NONE);
        mViewToType.put(mPhotos, ContentType.PHOTO);
        mViewToType.put(mFile, ContentType.FILE);
        mViewToType.put(mClipboard, ContentType.CLIPBOARD);
        mViewToType.put(mRight, ContentType.NONE);

        mPhotos.setOnClickListener(mItemOnClickListener);
        mFile.setOnClickListener(mItemOnClickListener);
        mClipboard.setOnClickListener(mItemOnClickListener);

        // update icon content
        mPhotos.setIconContentPaddingTop(mTopbarPhotoIconContentPaddingTop);
        mPhotoManager = RecentPhotoManager.getInstance(mContext);

        mFile.setIconContentPaddingTop(mTopbarFileIconContentPaddingTop);

        mFileManager = RecentFileManager.getInstance(mContext);
        mClipManager = RecentClipManager.getInstance(mContext);

        mShadowLine = findViewById(R.id.top_view_shadow_line);
    }

    public void requestStatus(SidebarStatus status) {
        if (status == SidebarStatus.NORMAL) {
            resumeToNormal();
        } else if (status == SidebarStatus.UNNAME) {
            dimAll();
        }
    }

    private View.OnClickListener mItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!(v instanceof TopItemView)) {
                // error !
                return;
            }
            if (!AnimStatusManager.getInstance().canShowContentView()) {
                AnimStatusManager.getInstance().dumpStatus();
                return;
            }

            TopItemView itemView = (TopItemView) v;
            if (mController.getCurrentContentType() == ContentType.NONE) {
                AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_CLICK, true);
                AnimTimeLine animTimeLine = new AnimTimeLine();
                mController.showContent(mViewToType.get(itemView));
                for (ITopItem view : mViewToType.keySet()) {
                    if (view == itemView) {
                        animTimeLine.addTimeLine(view.highlight());
                    } else {
                        animTimeLine.addTimeLine(view.dim());
                    }
                }
                animTimeLine.setAnimListener(new AnimListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onComplete(int type) {
                        AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_CLICK, false);
                    }
                });
                animTimeLine.start();
            } else {
                if (mController.getCurrentContentType() == mViewToType.get(itemView)) {
                    mController.dismissContent(true);
                    resumeToNormal();
                } else {
                    // never happen !
                }
            }
        }
    };

    public void dimAll(){
        AnimTimeLine timeLine = new AnimTimeLine();
        for (ITopItem view : mViewToType.keySet()) {
            timeLine.addTimeLine(view.dim());
        }
        timeLine.start();
    }

    public void resumeToNormal() {
        if (AnimStatusManager.getInstance().getStatus(AnimStatusManager.ON_TOP_VIEW_RESUME)) {
            log.error("ON_TOP_VIEW_RESUME is true, return !");
            return;
        }
        AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_RESUME, true);
        AnimTimeLine timeLine = new AnimTimeLine();
        for (ITopItem view : mViewToType.keySet()) {
            timeLine.addTimeLine(view.resume());
        }
        timeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onComplete(int type) {
                AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_RESUME, false);
            }
        });
        timeLine.start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (mController.getCurrentContentType() != ContentType.NONE) {
                if (AnimStatusManager.getInstance().canShowContentView()) {
                    Utils.resumeSidebar(mContext);
                    log.error("content not none ! resume sidebar...");
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private AnimTimeLine mEnterAnimTimeLine = null;

    private void doAnimWhenEnter() {
        mShadowLine.setVisibility(View.INVISIBLE);
        int time = 200;
        int height = getHeight();
        int fromY = -height;
        mPhotos.setAlpha(0);
        mFile.setAlpha(0);
        mClipboard.setAlpha(0);
        Vector3f moveFrom = new Vector3f(0, fromY);
        Vector3f moveTo = new Vector3f();
        Anim photoMove = new Anim(mPhotos, Anim.MOVE, time, Anim.CUBIC_OUT, moveFrom, moveTo);
        Anim fileMove = new Anim(mFile, Anim.MOVE, time, Anim.CUBIC_OUT, moveFrom, moveTo);
        Anim clipboardMove = new Anim(mClipboard, Anim.MOVE, time, Anim.CUBIC_OUT, moveFrom, moveTo);

        Vector3f alphaFrom = new Vector3f();
        Vector3f alphaTo = new Vector3f(0, 0, 1);
        Anim photoAlpha = new Anim(mPhotos, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, alphaFrom, alphaTo);
        Anim fileAlpha = new Anim(mFile, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, alphaFrom, alphaTo);
        Anim clipboardAlpha = new Anim(mClipboard, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, alphaFrom, alphaTo);

        Anim showShadowLine = new Anim(mShadowLine, Anim.TRANSPARENT, 30, Anim.CUBIC_OUT, alphaFrom, alphaTo);
        showShadowLine.setListener(new AnimListener() {
            @Override
            public void onStart() {
                mShadowLine.setVisibility(VISIBLE);
            }

            @Override
            public void onComplete(int type) {
                mShadowLine.setAlpha(1);
            }
        });
        showShadowLine.setDelay(170);

        setVisibility(View.INVISIBLE);
        Anim showBgShadow = new Anim(this, Anim.TRANSPARENT, 200, Anim.CUBIC_OUT, alphaFrom, alphaTo);
        showBgShadow.setListener(new AnimListener() {
            @Override
            public void onStart() {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onComplete(int type) {
                setAlpha(1);
            }
        });
        showBgShadow.setDelay(120);

        mEnterAnimTimeLine = new AnimTimeLine();
        mEnterAnimTimeLine.addAnim(photoMove);
        mEnterAnimTimeLine.addAnim(fileMove);
        mEnterAnimTimeLine.addAnim(clipboardMove);
        mEnterAnimTimeLine.addAnim(photoAlpha);
        mEnterAnimTimeLine.addAnim(fileAlpha);
        mEnterAnimTimeLine.addAnim(clipboardAlpha);
        mEnterAnimTimeLine.addAnim(showShadowLine);
        mEnterAnimTimeLine.addAnim(showBgShadow);
        mEnterAnimTimeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
                AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_ENTER, true);
            }

            @Override
            public void onComplete(int type) {
                if (mEnterAnimTimeLine != null) {
                    AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_ENTER, false);
                    setBackgroundResource(R.drawable.background);
                    mPhotos.setAlpha(1);
                    mFile.setAlpha(1);
                    mClipboard.setAlpha(1);

                    mPhotos.setTranslationY(0);
                    mFile.setTranslationY(0);
                    mClipboard.setTranslationY(0);
                    if (SidebarController.getInstance(mContext).getSidebarStatus() == SidebarStatus.UNNAME) {
                        dimAll();
                    }
                    mEnterAnimTimeLine = null;
                }
            }
        });
        mEnterAnimTimeLine.start();
    }

    private AnimTimeLine mExitAnimTimeLine = null;

    private void doAnimWhenExit() {
        setBackgroundResource(android.R.color.transparent);
        mShadowLine.setVisibility(View.INVISIBLE);
        int time = 200;
        Anim move = new Anim(this, Anim.MOVE, time, 0, new Vector3f(), new Vector3f(0, - getHeight()));
        Anim alpha = new Anim(this, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, new Vector3f(0, 0, 1), new Vector3f());

        mExitAnimTimeLine = new AnimTimeLine();
        mExitAnimTimeLine.addAnim(move);
        mExitAnimTimeLine.addAnim(alpha);

        mExitAnimTimeLine.setAnimListener(new AnimListener() {
            @Override
            public void onStart() {
                AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_EXIT, true);
            }

            @Override
            public void onComplete(int type) {
                if (mExitAnimTimeLine != null) {
                    AnimStatusManager.getInstance().setStatus(AnimStatusManager.ON_TOP_VIEW_EXIT, false);
                    resumeToNormal();
                    setTranslationY(0);
                    setAlpha(1);
                    setVisibility(View.GONE);
                    mExitAnimTimeLine = null;
                }
            }
        });
        mExitAnimTimeLine.start();
    }

    public void show(boolean show) {
        if (show) {
            if (mExitAnimTimeLine != null) {
                log.error("mExitAnimTimeLine not null");
                mExitAnimTimeLine.cancel();
            }
            setVisibility(View.VISIBLE);
            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);
                    doAnimWhenEnter();
                }
            });
        } else {
            if (mEnterAnimTimeLine != null) {
                mEnterAnimTimeLine.cancel();
            }
            doAnimWhenExit();
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {
            mDisableView.resume().start();
        } else {
            mDisableView.dim().start();
        }
    }
}
