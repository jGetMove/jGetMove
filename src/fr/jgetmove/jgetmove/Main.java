package fr.jgetmove.jgetmove;

import fr.jgetmove.jgetmove.database.Database;
import fr.jgetmove.jgetmove.io.Input;

public class Main{
	public static void main(String[] args){

		Input inputObj = new Input("src/assets/test.dat");
		Input inputTime = new Input("src/assets/testtimeindex.dat");

		Database database = new Database(inputObj,inputTime);
		System.out.println("Helloktamer");

	}
}