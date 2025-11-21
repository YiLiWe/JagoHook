package com.xposed.yok.activity.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OrderAdapter<T,  VB extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VB> {
    private final List<T> list;

    public OrderAdapter(List<T> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VB onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull VB holder, int position) {
        T bean = list.get(position);
        onBindViewHolder( bean, position);
    }



    public void onBindViewHolder(T ban, int position) {

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
