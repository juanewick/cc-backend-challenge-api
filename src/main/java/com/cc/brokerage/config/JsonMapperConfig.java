package com.cc.brokerage.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.MutableCoercionConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.math.BigDecimal;

@Configuration
public class JsonMapperConfig {

    @Bean
    JsonMapperBuilderCustomizer rejectMismatchedScalars() {
        return builder -> {
            fail(builder, LogicalType.Integer,
                    CoercionInputShape.String,
                    CoercionInputShape.Float,
                    CoercionInputShape.Boolean,
                    CoercionInputShape.Array,
                    CoercionInputShape.Object,
                    CoercionInputShape.EmptyArray,
                    CoercionInputShape.EmptyObject,
                    CoercionInputShape.EmptyString);
            fail(builder, LogicalType.Float,
                    CoercionInputShape.String,
                    CoercionInputShape.Boolean,
                    CoercionInputShape.Array,
                    CoercionInputShape.Object,
                    CoercionInputShape.EmptyArray,
                    CoercionInputShape.EmptyObject,
                    CoercionInputShape.EmptyString);
            fail(builder, LogicalType.Textual,
                    CoercionInputShape.Integer,
                    CoercionInputShape.Float,
                    CoercionInputShape.Boolean,
                    CoercionInputShape.Array,
                    CoercionInputShape.Object,
                    CoercionInputShape.EmptyArray,
                    CoercionInputShape.EmptyObject);
            fail(builder, LogicalType.Enum,
                    CoercionInputShape.Integer,
                    CoercionInputShape.Float,
                    CoercionInputShape.Boolean,
                    CoercionInputShape.Array,
                    CoercionInputShape.Object,
                    CoercionInputShape.EmptyArray,
                    CoercionInputShape.EmptyObject,
                    CoercionInputShape.EmptyString);
            // Amounts and prices travel as JSON strings ("800.00"), like every amount the API returns, so a
            // decimal can be read from text. Only BigDecimal gets this: other numbers stay strict.
            builder.withCoercionConfig(BigDecimal.class, config -> config.setCoercion(CoercionInputShape.String, CoercionAction.TryConvert));
        };
    }

    private static void fail(JsonMapper.Builder builder, LogicalType type, CoercionInputShape... shapes) {
        builder.withCoercionConfig(type, config -> {
            for (CoercionInputShape shape : shapes) {
                fail(config, shape);
            }
        });
    }

    private static void fail(MutableCoercionConfig config, CoercionInputShape shape) {
        config.setCoercion(shape, CoercionAction.Fail);
    }
}
