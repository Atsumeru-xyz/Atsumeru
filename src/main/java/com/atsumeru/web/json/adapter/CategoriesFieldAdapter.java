package com.atsumeru.web.json.adapter;

import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.util.GUType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

public class CategoriesFieldAdapter extends StringListBidirectionalAdapter {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        if (GUString.isEmpty(src)) {
            return null;
        }

        String[] array = src.split(",");
        if (GUArray.isEmpty(array)) {
            return null;
        }

        JsonArray jsonArray = new JsonArray();
        for (String value : array) {
            long categoryDbId = GUType.getLongDef(CategoryRepository.getRealIdFromCategoryDbId(value), -1);
            Category category = CategoryRepository.getCategoryByDbId(categoryDbId);
            if (category != null) {
                jsonArray.add(category.getCategoryId());
            }
        }

        return jsonArray;
    }
}