package com.raka.mealmate.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.raka.mealmate.R;

/**
 * Provides swipe-to-delete and swipe-to-check functionality for RecyclerView items
 */
public class SwipeToGestureCallback extends ItemTouchHelper.SimpleCallback {

    public interface SwipeActionListener {
        void onSwipeDelete(int position);
        void onSwipeToggleCheck(int position);
    }

    private final SwipeActionListener listener;
    private final Drawable deleteIcon;
    private final Drawable checkIcon;
    private final ColorDrawable deleteBackground;
    private final ColorDrawable checkBackground;
    private final int iconMargin;
    private final boolean swipeLeftEnabled;
    private final boolean swipeRightEnabled;
    private final Paint clearPaint;

    public SwipeToGestureCallback(Context context, SwipeActionListener listener) {
        this(context, listener, true, true);
    }

    public SwipeToGestureCallback(Context context, SwipeActionListener listener, 
                                  boolean swipeLeftEnabled, boolean swipeRightEnabled) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        this.swipeLeftEnabled = swipeLeftEnabled;
        this.swipeRightEnabled = swipeRightEnabled;
        
        deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        checkIcon = ContextCompat.getDrawable(context, android.R.drawable.checkbox_on_background);
        deleteBackground = new ColorDrawable(Color.parseColor("#FF5252"));
        checkBackground = new ColorDrawable(Color.parseColor("#4CAF50"));
        iconMargin = context.getResources().getDimensionPixelSize(R.dimen.swipe_icon_margin);
        
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, 
                         @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't support drag & drop
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Disable swipe for header items
        if (viewHolder.getItemViewType() == 0) {
            return 0;
        }
        
        int swipeFlags = 0;
        if (swipeLeftEnabled) {
            swipeFlags |= ItemTouchHelper.LEFT;
        }
        if (swipeRightEnabled) {
            swipeFlags |= ItemTouchHelper.RIGHT;
        }
        
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeDelete(position);
        } else if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipeToggleCheck(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        
        // Ensure the item has positive dimensions
        if (itemHeight <= 0) {
            return;
        }
        
        boolean isCancelled = dX == 0f && !isCurrentlyActive;
        
        if (isCancelled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }
        
        // Swipe to delete (left)
        if (dX < 0) {
            // Draw red background
            deleteBackground.setBounds(
                    itemView.getRight() + (int) dX,
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
            deleteBackground.draw(c);
            
            // Calculate position for the delete icon
            int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
            int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
            
            // Draw delete icon
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.draw(c);
        }
        
        // Swipe to check/uncheck (right)
        else if (dX > 0) {
            // Draw green background
            checkBackground.setBounds(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getLeft() + (int) dX,
                    itemView.getBottom()
            );
            checkBackground.draw(c);
            
            // Calculate position for the check icon
            int iconTop = itemView.getTop() + (itemHeight - checkIcon.getIntrinsicHeight()) / 2;
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = itemView.getLeft() + iconMargin + checkIcon.getIntrinsicWidth();
            int iconBottom = iconTop + checkIcon.getIntrinsicHeight();
            
            // Draw check icon
            checkIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            checkIcon.draw(c);
        }
        
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
    
    private void clearCanvas(Canvas c, float left, float top, float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}
