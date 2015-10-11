package edu.uri.egr.wiot468template;

import edu.uri.egr.hermes.Hermes;

/**
 * Created by cody on 10/8/15.
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Hermes.Config config = new Hermes.Config()
                .enableDebug(true);

        Hermes.init(this, config);
    }
}
