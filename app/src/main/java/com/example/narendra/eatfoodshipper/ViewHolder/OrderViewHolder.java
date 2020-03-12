package com.example.narendra.eatfoodshipper.ViewHolder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.narendra.eatfoodshipper.R;

public class OrderViewHolder extends RecyclerView.ViewHolder {

    public TextView txtOrderId, txtOrderStatus, txtOrderPhone, txtOrderAddress,txtOrderDate;
    public Button btnshipping;



    public OrderViewHolder(View itemView) {
        super(itemView);

        txtOrderId =  itemView.findViewById(R.id.order_name);
        txtOrderStatus =  itemView.findViewById(R.id.order_status);
        txtOrderPhone =  itemView.findViewById(R.id.order_phone);
        txtOrderAddress = itemView.findViewById(R.id.order_ship_to);
        txtOrderDate=itemView.findViewById(R.id.order_date);

        btnshipping=itemView.findViewById(R.id.btnShipping);


    }



}

