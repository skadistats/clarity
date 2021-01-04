package skadistats.clarity.decoder.s2.field;

public interface DecoderProperties {

    Integer getEncodeFlags();
    Integer getBitCount();
    Float getLowValue();
    Float getHighValue();
    String getEncoderType();

    int getEncodeFlagsOrDefault(int defaultValue);
    int getBitCountOrDefault(int defaultValue);
    float getLowValueOrDefault(float defaultValue);
    float getHighValueOrDefault(float defaultValue);

    DecoderProperties DEFAULT = new DecoderProperties() {
        @Override
        public Integer getEncodeFlags() {
            return null;
        }

        @Override
        public Integer getBitCount() {
            return null;
        }

        @Override
        public Float getLowValue() {
            return null;
        }

        @Override
        public Float getHighValue() {
            return null;
        }

        @Override
        public String getEncoderType() {
            return null;
        }

        @Override
        public int getEncodeFlagsOrDefault(int defaultValue) {
            return defaultValue;
        }

        @Override
        public int getBitCountOrDefault(int defaultValue) {
            return defaultValue;
        }

        @Override
        public float getLowValueOrDefault(float defaultValue) {
            return defaultValue;
        }

        @Override
        public float getHighValueOrDefault(float defaultValue) {
            return defaultValue;
        }
    };

}
