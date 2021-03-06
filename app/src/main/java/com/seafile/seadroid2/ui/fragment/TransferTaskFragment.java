package com.seafile.seadroid2.ui.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.google.common.collect.Lists;
import com.seafile.seadroid2.R;
import com.seafile.seadroid2.transfer.TransferService;
import com.seafile.seadroid2.transfer.TransferTaskInfo;
import com.seafile.seadroid2.ui.ActionModeCallback;
import com.seafile.seadroid2.ui.ToastUtils;
import com.seafile.seadroid2.ui.activity.TransferActivity;
import com.seafile.seadroid2.ui.adapter.TransferTaskAdapter;

import java.util.List;

/**
 * Base class for transfer task fragments
 *
 */
public abstract class TransferTaskFragment extends SherlockListFragment
        implements ActionModeCallback.ActionModeOperationListener {
    private String DEBUG_TAG = "TransferTaskFragment";

    protected TransferTaskAdapter adapter;
    protected TransferActivity mActivity = null;
    protected ListView mTransferTaskListView;
    protected LinearLayout mTaskActionBar;
    protected LinearLayout mTaskDeleteBtn;
    protected LinearLayout mTaskRestartBtn;
    protected TextView emptyView;
    private View mListContainer;
    private View mProgressContainer;
    protected final Handler mTimer = new Handler();
    private TaskActionListener listener = new TaskActionListener();
    protected TransferService txService = null;
    private ActionMode mActionMode;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (TransferActivity) activity;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.transfer_task_fragment, container, false);
        mTransferTaskListView = (ListView) root.findViewById(android.R.id.list);
        mTransferTaskListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    mActionMode = getSherlockActivity().startActionMode(new ActionModeCallback(TransferTaskFragment.this));
                }

                return true;
            }
        });

        mListContainer =  root.findViewById(R.id.listContainer);
        mProgressContainer = root.findViewById(R.id.progressContainer);
        mTaskActionBar = (LinearLayout) root.findViewById(R.id.task_action_container);
        mTaskDeleteBtn = (LinearLayout) root.findViewById(R.id.task_action_delete);
        mTaskRestartBtn = (LinearLayout) root.findViewById(R.id.task_action_restart);
        mTaskDeleteBtn.setOnClickListener(listener);
        mTaskRestartBtn.setOnClickListener(listener);
        emptyView = (TextView) root.findViewById(R.id.empty);
        return root;
    }

    class TaskActionListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.task_action_delete:
                    List<Integer> ids = adapter.getSelectedIds();
                    if (ids != null) {
                        if (ids.size() == 0) {
                            ToastUtils.show(mActivity, R.string.action_mode_no_items_selected);
                            return;
                        }

                        deleteSelectedItems(convertToTaskIds(ids));
                        deselectItems();
                    }
                    break;
                case R.id.task_action_restart:
                    List<Integer> restartIds = adapter.getSelectedIds();
                    if (restartIds != null) {
                        if (restartIds.size() == 0) {
                            ToastUtils.show(mActivity, R.string.action_mode_no_items_selected);
                            return;
                        }

                        restartSelectedItems(convertToTaskIds(restartIds));
                        deselectItems();
                    }
                    break;
            }
        }
    }

    private List<Integer> convertToTaskIds(List<Integer> positions) {
        List<Integer> taskIds = Lists.newArrayList();
        for (int position : positions) {
            TransferTaskInfo tti = adapter.getItem(position);
            taskIds.add(tti.taskID);
        }

        return taskIds;
    }
    /**
     * select all items
     */
    @Override
    public void selectItems() {
        if (adapter == null) return;

        adapter.selectAllItems();
        updateContextualActionBar();

    }

    /**
     * deselect all items
     */
    @Override
    public void deselectItems() {
        if (adapter == null) return;

        adapter.deselectAllItems();
        updateContextualActionBar();
    }

    @Override
    public void onActionModeStarted() {
        if (adapter == null) return;

        adapter.actionModeOn();
        Animation bottomUp = AnimationUtils.loadAnimation(getActivity(),
                R.anim.bottom_up);
        mTaskActionBar.startAnimation(bottomUp);
        mTaskActionBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onActionModeDestroy() {
        if (adapter == null) return;

        adapter.deselectAllItems();
        adapter.actionModeOff();
        Animation bottomDown = AnimationUtils.loadAnimation(getActivity(),
                R.anim.bottom_down);
        mTaskActionBar.startAnimation(bottomDown);
        mTaskActionBar.setVisibility(View.GONE);

        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
        mActionMode = null;
    }

    protected abstract void deleteSelectedItems(List<Integer> ids);

    protected abstract void restartSelectedItems(List<Integer> ids);

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        showLoading(true);

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Toast.makeText(mActivity, "Stop loading animations", Toast.LENGTH_LONG).show();
            showLoading(false);

            TransferService.TransferBinder binder = (TransferService.TransferBinder) service;
            txService = binder.getService();
            if (isNeedUpdateProgress()) {
                mTransferTaskListView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                setUpTransferList();
                startTimer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            txService = null;
        }
    };

    protected abstract List<? extends TransferTaskInfo> getTransferTaskInfos();

    protected abstract void setUpTransferList();

    @Override
    public void onResume() {
        super.onResume();
        mTransferTaskListView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // bind transfer service
        Intent bIntent = new Intent(mActivity, TransferService.class);
        mActivity.bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    protected abstract boolean isNeedUpdateProgress();

    @Override
    public void onStop() {
        super.onStop();
        stopTimer();
        if (txService != null) {
            mActivity.unbindService(mConnection);
            txService = null;
        }
    }

    // refresh list by mTimer
    private void startTimer() {
        Log.d(DEBUG_TAG, "timer started");
        mTimer.postDelayed(new Runnable() {

            @Override
            public void run() {
                adapter.setTransferTaskInfos(getTransferTaskInfos());
                adapter.notifyDataSetChanged();
                //Log.d(DEBUG_TAG, "timer post refresh signal " + System.currentTimeMillis());
                mTimer.postDelayed(this, 1 * 1000);
            }
        }, 1 * 1000);
    }

    public void stopTimer() {
        mTimer.removeCallbacksAndMessages(null);
    }

    private void showLoading(boolean show) {
        if (mActivity == null)
            return;

        if (show) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));

            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        } else {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));

            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            // add or remove selection for current list item
            if (adapter == null) return;

            adapter.toggleSelection(position);
            updateContextualActionBar();
        }
    }

    /**
     *  update state of contextual action bar
     */
    public void updateContextualActionBar() {
        boolean itemsChecked = adapter.getCheckedItemCount() > 0;

        if (itemsChecked && mActionMode == null) {
            // there are some selected items, start the actionMode
            mActionMode = getSherlockActivity().startActionMode(new ActionModeCallback(this));
            adapter.actionModeOn();
            Animation bottomUp = AnimationUtils.loadAnimation(getActivity(),
                    R.anim.bottom_up);
            mTaskActionBar.startAnimation(bottomUp);
            mTaskActionBar.setVisibility(View.VISIBLE);
        }


        if (mActionMode != null) {
            // Log.d(DEBUG_TAG, "mActionMode.setTitle " + adapter.getCheckedItemCount());
            mActionMode.setTitle(getResources().getQuantityString(
                    R.plurals.transfer_list_items_selected,
                    adapter.getCheckedItemCount(),
                    adapter.getCheckedItemCount()));
        }

    }

}
