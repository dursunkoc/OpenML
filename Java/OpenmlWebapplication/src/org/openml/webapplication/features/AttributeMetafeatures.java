package org.openml.webapplication.features;

import org.openml.apiconnector.xml.DataQuality;
import org.openml.webapplication.attributeCharacterization.AttributeCharacterizer;
import weka.core.Instances;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeMetafeatures {
    private List<AttributeCharacterizer> attributeCharacterizers = new ArrayList<>();
    public AttributeMetafeatures(int numAttributes){
        for (int indexNr =0; indexNr < numAttributes; indexNr++) {
            AttributeCharacterizer attributeCharacterizer = new AttributeCharacterizer(indexNr);
            attributeCharacterizers.add(attributeCharacterizer);
        }
    }

    public  List<DataQuality.Quality> qualityResultToList(Map<String, QualityResult> map, Integer start, Integer size) {
        List<DataQuality.Quality> result = new ArrayList<>();
        for(String quality : map.keySet()) {
            Integer end = start != null ? start + size : null;
            QualityResult qualityResult = map.get(quality);
            result.add(new DataQuality.Quality(quality, qualityResult.value + "", start, end, qualityResult.index));
        }
        return result;
    }

    public Map<String, QualityResult> characterize(Instances instances, AttributeCharacterizer characterizer){
        Map<String, Double> values =characterizer.characterize(instances);
        Map <String, QualityResult> result = new HashMap<>();
        values.forEach((s,v) -> result.put(s, new QualityResult(v, characterizer.getIndex())));
        return result;
    }

    public List<AttributeCharacterizer> getAttributeCharacterizers(){
        return attributeCharacterizers;
    }

    public static int numberOfAttributeMetafeatures(){
        return AttributeCharacterizer.ids.length;
    }
}
