package xyz.atsumeru.web.json.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import xyz.atsumeru.web.model.database.Category;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.TypeUtils;

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