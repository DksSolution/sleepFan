package com.sleep.fan.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.sleep.fan.ActivityUnpurchasedItem;
import com.sleep.fan.R;
import com.sleep.fan.fragments.SoundsFragment;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;

import java.util.ArrayList;

public class UnPurchasedAdapter extends RecyclerView.Adapter {

    SoundsFragment mContext;
    ListDataModel listDataModel;
    ArrayList<DataModel> listUnPurchases;

    public UnPurchasedAdapter(SoundsFragment context, ArrayList<DataModel> listUnPurchases) {
        this.mContext = context;
        this.listUnPurchases = listUnPurchases;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_unpurchased_item, parent, false);
        RecyclerView.ViewHolder viewHolder = new ViewHolder(view, mContext.getContext());
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(!listUnPurchases.get(position).isPurchased) {
            Log.e("UnPurchasedAdapter", "UnPurchasedAdapter: " + listUnPurchases.get(position).name );
            ((ViewHolder) holder).tv_title.setText(listUnPurchases.get(position).name);
            ((ViewHolder) holder).item.setImageResource(mContext.getResources().getIdentifier("@drawable/" + listUnPurchases.get(position).icon, null, mContext.getContext().getPackageName()));
            ((ViewHolder) holder).item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mContext.openDemoDialog(listUnPurchases.get(position).position, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return this.listUnPurchases.size();
    }


    private class ViewHolder extends RecyclerView.ViewHolder{
        ShapeableImageView item;
        TextView tv_title;
        public ViewHolder(final View itemView, Context context) {
            super(itemView);
            item = itemView.findViewById(R.id.item);
            tv_title = itemView.findViewById(R.id.tv_title);
        }
    }
}
