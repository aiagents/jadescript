package tests.nativeops;

import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import jadescript.java.Jadescript;

public class StartNativeOpsTest {
	public static void main(String[] args) throws StaleProxyException {
		ContainerController container = Jadescript.newMainContainer();
		Jadescript.bindNative(reverse.class, MyReverseImplementation.class);
		Jadescript.bindNative(sysout.class, MySysoutImplementation.class);
		NativeOps.create(container, "NativeOps");
	}
}