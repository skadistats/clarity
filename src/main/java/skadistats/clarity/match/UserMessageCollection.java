package skadistats.clarity.match;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;

import java.util.List;

public class UserMessageCollection extends BaseCollection<UserMessageCollection, GeneratedMessage> {

    public <T> List<T> getByClass(final Class<T> clazz) {
        return (List<T>) Lists.newArrayList(
          iteratorForPredicate(new Predicate<GeneratedMessage>() {
              @Override
              public boolean apply(GeneratedMessage generatedMessage) {
                  return clazz.equals(generatedMessage.getClass());
              }
          })
        );
    }
}
