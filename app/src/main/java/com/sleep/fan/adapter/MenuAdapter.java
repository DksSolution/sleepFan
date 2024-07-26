package com.sleep.fan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sleep.fan.ActivityMenu;
import com.sleep.fan.R;
import com.sleep.fan.fragments.FansFragment;
import com.sleep.fan.model.DataModel;
import com.sleep.fan.model.ListDataModel;

import java.util.ArrayList;

public class MenuAdapter extends RecyclerView.Adapter {

    FansFragment mContext;
    ArrayList<DataModel> listPurchases;

    public MenuAdapter(FansFragment context, ArrayList<DataModel> listPurchases) {
        this.mContext = context;
        this.listPurchases = listPurchases;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_menu, parent, false);
        RecyclerView.ViewHolder viewHolder = new ViewHolder(view, mContext.requireActivity());
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).item.setImageResource(mContext.getResources().getIdentifier("@drawable/" + listPurchases.get(position).icon, null, mContext.requireActivity().getPackageName()));
        ((ViewHolder) holder).item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContext.PrepareFanLaunch(position);
            }
        });
    }


    @Override
    public int getItemCount() {
        return listPurchases.size();
    }


    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView item;

        public ViewHolder(final View itemView, Context context) {
            super(itemView);
            item = (ImageView) itemView.findViewById(R.id.item);
        }
    }
}
