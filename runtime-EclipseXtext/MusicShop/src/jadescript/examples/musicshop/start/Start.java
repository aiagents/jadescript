package jadescript.examples.musicshop.start;

import java.util.Scanner;

import jadescript.java.Jadescript;
import jadescript.util.JadescriptList;
import jade.wrapper.ContainerController;
import jadescript.examples.musicshop.agents.BuyerAgent;
import jadescript.examples.musicshop.agents.SellerAgent;

public class Start {
	// Main method that allows launching this test as a stand-alone program
	public static void main(String[] args) {
		try (Scanner scanner = new java.util.Scanner(System.in)) {
			ContainerController container = Jadescript.newMainContainer();
			String seller1Name = "seller1";
			String seller2Name = "seller2";
			String seller3Name = "seller3";
			String buyerName = "buyer";
			
			SellerAgent.create(container, seller1Name,
					JadescriptList.of(1, 3, 4),
					JadescriptList.of(10, 12, 20)
			);
			SellerAgent.create(container, seller2Name,
					JadescriptList.of(1, 2, 4),
					JadescriptList.of(8, 15, 10)
			);
			SellerAgent.create(container, seller3Name,
					JadescriptList.of(4),
					JadescriptList.of(25)
			);
			
			
			System.out.println("Press enter to start the buyer...");
			
			scanner.nextLine();
			
			BuyerAgent.create(container, buyerName, 
					4,
					JadescriptList.of(seller1Name, seller2Name, seller3Name)
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
