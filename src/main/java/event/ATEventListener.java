package event;

import java.util.EventListener;

/**
 * Created by Sharukh Hasan on 4/19/17.
 */
public interface ATEventListener extends EventListener {

    public void ATEvent(byte[] event);

}
