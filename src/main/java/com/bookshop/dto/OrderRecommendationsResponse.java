package com.bookshop.dto;

import com.bookshop.model.Order;

public record OrderRecommendationsResponse(Order order, String recommendations) {

}
