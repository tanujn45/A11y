package com.tanujn45.a11y;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

public class AccItem extends ConstraintLayout {
    private ImageView imageView;
    private TextView textView;
    private ImageButton closeButton;

    public interface OnItemRemovedListener {
        void onItemRemoved(AccItem accItem);
    }

    private OnItemRemovedListener onItemRemovedListener;

    public void setOnItemRemovedListener(OnItemRemovedListener onItemRemovedListener) {
        this.onItemRemovedListener = onItemRemovedListener;
    }

    public AccItem(Context context) {
        super(context);
        init(context);
    }

    public AccItem(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AccItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        LayoutInflater inflator = LayoutInflater.from(context);
        inflator.inflate(R.layout.acc_item_view, this);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        closeButton = findViewById(R.id.closeButton);

        closeButton.setOnClickListener(v -> {
            ViewGroup parentView = (ViewGroup) getParent();
            if (parentView != null) {
                parentView.removeView(AccItem.this);
                if (onItemRemovedListener != null)
                    onItemRemovedListener.onItemRemoved(AccItem.this);
            }
        });
    }

    public void setImage(int resId) {
        imageView.setImageResource(resId);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public String getText() {
        return textView.getText().toString();
    }
}
