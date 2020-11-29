package nl.praegus.fitnesse.slim.fixtures.util;

import nl.hsac.fitnesse.fixture.slim.*;

public class PauseTestFixture extends SlimFixture {

  public void pause() throws StopTestException {
    pause("<no message>");
  }
  public void pause(String msg) throws StopTestException {
    pause(msg, "Fitnesse - Pause");
  }
  public void pause(String msg, String title) throws StopTestException {
    if (!canPause()) {
      throw new StopTestException(false,
          "Pause was called from a Junit run. Aborting to avoid infinite waiting..");
    }
    
    int button = PausePopupDialog.pause(msg, title, new String[] { "Continue", "Stop test"});
    switch(button) {
    case 0: break;
    case 1:
      System.out.println("Test stopped by user.");
      throw new StopTestException(false, "Test stopped by user.");
    default:
      System.out.println("PausePopupDialog.pause() : unknown return value.");
    }
  }
  private boolean canPause() {
    String propValue = System.getProperties().getProperty("nodebug");
    if(null == propValue) return true;
    if(propValue.equalsIgnoreCase("true")) return false;
    return true;
  }  
}
