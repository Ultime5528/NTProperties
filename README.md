# NTProperties

## Installation avec Gradle
```
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'com.github.ultime5528:ntproperties:v0.1.0'
}
```

## Utilisation
```
import com.ultime5528.ntproperties;

public class Robot extends TimedRobot {
    
    private NTProperties props;
    
    @Override
    public void robotInit() {
        props = new NTProperties(K.class, true);
    }
    
    @Override
    public void robotPeriodic() {
        props.performChanges();
    }
    
}
```