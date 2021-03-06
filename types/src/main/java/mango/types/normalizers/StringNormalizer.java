package mango.types.normalizers;

import mango.types.TypeNormalizer;
import mango.types.exception.TypeNormalizationException;

public class StringNormalizer implements TypeNormalizer<String> {

    @Override
    public String normalize(String obj) throws TypeNormalizationException {
        return obj;
    }

    @Override
    public String denormalize(String str) throws TypeNormalizationException {
        return str;
    }

    @Override
    public String getAlias() {
        return "string";
    }

    @Override
    public Class resolves() {
        return String.class;
    }

    @Override
    public String fromString(String str) throws TypeNormalizationException {
        return str;
    }

    @Override
    public String asString(String obj) throws TypeNormalizationException {
        return obj;
    }
}
