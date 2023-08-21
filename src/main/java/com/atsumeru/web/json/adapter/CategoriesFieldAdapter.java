package com.atsumeru.web.json.adapter;

import com.atsumeru.web.model.database.Category;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.util.TypeUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;

import java.lang.reflect.Type;

public class CategoriesFieldAdapter extends StringListBidirectionalAdapter {

    @Override
    public JsonElement serialize(String src, Type typeOfSrc, JsonSerializationContext context) {
        if (StringUtils.isEmpty(src)) {
            return null;
        }

        String[] array = src.split(",");
        if (ArrayUtils.isEmpty(array)) {
            return null;
        }

        JsonArray jsonArray = new JsonArray();
        for (String value : array) {
            long categoryDbId = TypeUtils.getLongDef(CategoryRepository.getRealIdFromCategoryDbId(value), -1);
            Category category = CategoryRepository.getCategoryByDbId(categoryDbId);
            if (category != null) {
                jsonArray.add(category.getCategoryId());
            }
        }

        return jsonArray;
    }
}