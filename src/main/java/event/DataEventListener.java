package event;

import java.util.EventListener;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public interface DataEventListener extends EventListener {

    public void DataEvent(byte[] event);

}
