package com.ecanaveras.gde.waudio;

import java.util.Random;

/**
 * Created by ecanaveras on 04/08/2017.
 */

public class RandomTest {
    public static void main(String[] args) {
        String[] num_template = {"2", "1"};
        Random random = new Random();
        System.out.println(num_template[random.nextInt(num_template.length)]);
    }
}
