package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.zfdang.zsmth_android.models.GuidanceContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class GuidanceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private final String TAG = "Guidance Fragment";

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private RecyclerView mRecyclerView = null;
    private SwipeRefreshLayout mSwipeRefreshLayout = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public GuidanceFragment() {
    }

    @SuppressWarnings("unused")
    public static GuidanceFragment newInstance(int columnCount) {
        GuidanceFragment fragment = new GuidanceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_guidance, container, false);

        // http://sapandiwakar.in/pull-to-refresh-for-android-recyclerview-or-any-other-vertically-scrolling-view/
        // pull to refresh for android recyclerview
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView;
        mSwipeRefreshLayout.setOnRefreshListener(this);

        // http://blog.csdn.net/lmj623565791/article/details/45059587
        // 你想要控制Item间的间隔（可绘制），请通过ItemDecoration
        // 你想要控制Item增删的动画，请通过ItemAnimator
        // 你想要控制点击、长按事件，请自己写
        // item被按下的时候的highlight,这个是通过guidance item的backgroun属性来实现的 (android:background="@drawable/recyclerview_item_bg")
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.guidance_recycler_view);
        // Set the adapter
        if (mRecyclerView != null) {
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
            Context context = rootView.getContext();
            if (mColumnCount <= 1) {
                mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            mRecyclerView.setAdapter(new GuidanceRecyclerViewAdapter(GuidanceContent.ITEMS, mListener));
        }

        getActivity().setTitle("zSMTH - " + "首页导读");

        if(GuidanceContent.ITEMS.size() == 0){
            // only refresh guidance when there is no topic available
            RefreshGuidance();
        }
        return rootView;
    }

    public void RefreshGuidance(){
        SMTHHelper helper = SMTHHelper.getInstance();
        helper.wService.getGuidance()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                // convert ResponseBody to UTF-8 String first
                .map(new Func1<ResponseBody, String>() {
                    public String call(ResponseBody response) {
                        try {
                            String resp = SMTHHelper.DecodeWWWResponse(response.bytes());
                            Log.d(TAG, resp.length()+"");
                            return resp;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                })
                // map String to list of topics
                .flatMap(new Func1<String, Observable<Topic>>() {
                    @Override
                    public Observable<Topic> call(String s) {
                        return Observable.from(SMTHHelper.ParseHotTopics(s));
                    }
                })
                // add topics to guidance recyclerview
                .subscribe(new Action1<Topic>() {
                    // onNextAction
                    @Override
                    public void call(Topic topic) {
                        // add topic into GuidanceContent, and update RecyclerView
                        Log.d(TAG, topic.toString());
                        GuidanceContent.addItem(topic);
                        mRecyclerView.getAdapter().notifyItemInserted(GuidanceContent.ITEMS.size());
                    }
                }, new Action1<Throwable>() {
                    // onErrorAction
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, throwable.toString());
                        Toast.makeText(getActivity(), "连接错误，请检查网络.", Toast.LENGTH_LONG).show();
                    }
                });
        }


    // http://stackoverflow.com/questions/32604552/onattach-not-called-in-fragment
    // If you run your application on a device with API 23 (marshmallow) then onAttach(Context) will be called.
    // On all previous Android Versions onAttach(Activity) will be called.
    @Override
    public void onAttach(Activity activity) {
        this.onAttach((Context)activity);
    }

    //    @Override
    public void onAttach(Context context) {
        super.onAttach((Activity)context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh() {
        RefreshGuidance();
        mSwipeRefreshLayout.setRefreshing(false);
        Toast.makeText(getActivity(), "刷新完成!", Toast.LENGTH_SHORT).show();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Topic item);
    }
}
