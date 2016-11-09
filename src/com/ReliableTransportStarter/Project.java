package com.ReliableTransportStarter;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class Project {
    public final static void main(String[] argv) {
        final JFrame frame = new JFrame("Reliable Tranport Sim");

        //# msgs to Simulate
        final SpinnerNumberModel nummsgs = new SpinnerNumberModel(10, 2, 30, 1);
        JSpinner spinner1 = new JSpinner(nummsgs);
        JPanel Panel1 = new JPanel();
        Panel1.add(new JLabel("# msgs to simulate:"));
        Panel1.add(spinner1);

        // Packet loss prob
        double initial = 0.0, min = 0.0, max = 1.0, increment = 0.01;
        final SpinnerNumberModel model2 = new SpinnerNumberModel(initial, min, max, increment);
        JSpinner spinner2 = new JSpinner(model2);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner2, "0%");
        spinner2.setEditor(editor);
        JPanel Panel2 = new JPanel();
        Panel2.add(new JLabel("Packet loss probability"));
        Panel2.add(spinner2);

        // Packet corruption prob
        final SpinnerNumberModel model3 = new SpinnerNumberModel(initial, min, max, increment);
        JSpinner spinner3 = new JSpinner(model3);
        JSpinner.NumberEditor editor3 = new JSpinner.NumberEditor(spinner3, "0%");
        spinner3.setEditor(editor3);
        JPanel Panel3 = new JPanel();
        Panel3.add(new JLabel("Packet corruption probability"));
        Panel3.add(spinner3);

        // Avg time btwn msgs
        final SpinnerNumberModel model4 = new SpinnerNumberModel(1000, 0, 2000, 10);
        JSpinner spinner4 = new JSpinner(model4);
        JPanel Panel4 = new JPanel();
        Panel4.add(new JLabel("Average time between messages"));
        Panel4.add(spinner4);

        // Window Size
        final SpinnerNumberModel model5 = new SpinnerNumberModel(8, 0, 10, 1);
        JSpinner spinner5 = new JSpinner(model5);
        JPanel Panel5 = new JPanel();
        Panel5.add(new JLabel("Window size"));
        Panel5.add(spinner5);

        // Retransmission timeout
        final SpinnerNumberModel model6 = new SpinnerNumberModel(15, 0, 30, 1);
        JSpinner spinner6 = new JSpinner(model6);
        JPanel Panel6 = new JPanel();
        Panel6.add(new JLabel("Retransmission timeout"));
        Panel6.add(spinner6);

        // Trace level
        final SpinnerNumberModel model7 = new SpinnerNumberModel(0, 0, 3, 1);
        JSpinner spinner7 = new JSpinner(model7);
        JPanel Panel7 = new JPanel();
        Panel7.add(new JLabel("Trace level"));
        Panel7.add(spinner7);

        // Random Seed
        final SpinnerNumberModel model8 = new SpinnerNumberModel(65, 1, 99, 1);
        JSpinner spinner8 = new JSpinner(model8);
        JPanel Panel8 = new JPanel();
        Panel8.add(new JLabel("Random Seed"));
        Panel8.add(spinner8);


        JPanel finishedPanel = new JPanel();
        JButton finishedButton = new JButton("Begin Simulation");
        finishedPanel.add(finishedButton);


        Container content = frame.getContentPane();
        content.setLayout(new GridLayout(0, 1));
        content.add(Panel1);
        content.add(Panel2);
        content.add(Panel3);
        content.add(Panel4);
        content.add(Panel5);
        content.add(Panel6);
        content.add(Panel7);
        content.add(Panel8);
        content.add(finishedPanel);

        finishedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int num = nummsgs.getNumber().intValue();
                double lossprob = model2.getNumber().doubleValue();
                double corrprob = model3.getNumber().doubleValue();
                double msgtime = model4.getNumber().doubleValue();
                int windowSize = model5.getNumber().intValue();
                double retrans = model6.getNumber().doubleValue();
                int trace = model7.getNumber().intValue();
                int seed = model8.getNumber().intValue();
                starter(num, lossprob, corrprob, msgtime, windowSize, retrans, trace, seed);
                frame.dispose();
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 700);
        frame.setVisible(true);
    }


    public static void starter(int nsim, double loss, double corrupt, double delay, int windowsize, double timeout, int trace, int seed) {
        StudentNetworkSimulator simulator;

        String buffer = "";
        File outputfile = new File("OutputFile");
        BufferedReader stdIn = new BufferedReader(
                new InputStreamReader(System.in));

        System.out.println("Network Simulator v1.0");
        System.out.println("Number of messages to simulate: " + nsim);
        System.out.println("Packet loss probability: " + loss);
        System.out.println("Packet corruption probability: " + corrupt);
        System.out.println("Average time between messages from sender's layer 5: " + delay);
        System.out.println("Window size: " + windowsize);
        System.out.println("Retransmission timeout: " + timeout);
        System.out.println("Trace level: " + trace);
        System.out.println("Random seed: " + seed);
        
        
	/* while (nsim < 1)
        {
            /*System.out.println("Enter number of messages to simulate (> 0): " +
                             "[10] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                nsim = 10;
            }
            else
            {            
                try
                {
                    nsim = Integer.parseInt(buffer);
                }
                catch (NumberFormatException nfe)
                {
                    nsim = -1;
                }
            }
	    } 
	    
		
        while (loss < 0)
        {
            System.out.println("Enter packet loss probability (0.0 for no " +
                             "loss): [0.0] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                loss = 0;
            }
            else
            {            
                try
                {
                    loss = (Double.valueOf(buffer)).doubleValue();
                }
                catch (NumberFormatException nfe)
                {
                    loss = -1;
                }
            }
        }            

        while (corrupt < 0)
        {
            System.out.println("Enter packet corruption probability (0.0 " +
                             "for no corruption): [0.0] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                corrupt = 0;
            }
            else
            {            
                try
                {
                    corrupt = (Double.valueOf(buffer)).doubleValue();
                }
                catch (NumberFormatException nfe)
                {
                    corrupt = -1;
                }
            }
        }            

        while (delay <= 0)
        {
            System.out.println("Enter average time between messages from " +
                             "sender's layer 5 (> 0.0): [1000] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                delay = 1000;
            }
            else
            {            
                try
                {
                    delay = (Double.valueOf(buffer)).doubleValue();
                }
                catch (NumberFormatException nfe)
                {
                    delay = -1;
                }
            }
        }            

         while (windowsize < 1)
        {
            System.out.println("Enter window size (> 0): [8] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                windowsize = 8;
            }
            else
            {            
                try
                {
                    windowsize = Integer.parseInt(buffer);
                }
                catch (NumberFormatException nfe)
                {
                    windowsize = -1;
                }
            }
        }

	while (timeout <= 0)
        {
            System.out.println("Enter retransmission timeout (>0.0) [15.0] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                timeout = 15.0;
            }
            else
            {            
                try
                {
                    timeout = (Double.valueOf(buffer)).doubleValue();
                }
                catch (NumberFormatException nfe)
                {
                    timeout = -1;
                }
            }
        }            

        while (trace < 0)
        {
            System.out.println("Enter trace level (>= 0): [0] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
                trace = 0;
            }
            else
            {            
                try
                {
                    trace = Integer.parseInt(buffer);
                }
                catch (NumberFormatException nfe)
                {
                    trace = -1;
                }
            }
        }

        while (seed < 1)
        {
            System.out.println("Enter random seed: [0] ");
            try
            {
                buffer = stdIn.readLine();
            }
            catch (IOException ioe)
            {
                System.out.println("IOError reading your input!");
                System.exit(1);
            }
            
            if (buffer.equals(""))
            {
		seed = 0;
            }
            else
            {            
                try
                {
                    seed = (Integer.valueOf(buffer)).intValue();
                }
                catch (NumberFormatException nfe)
                {
                    seed = -1;
                }
            }
        }
	*/
        simulator = new StudentNetworkSimulator(nsim, loss, corrupt, delay,
                trace, seed, windowsize, timeout);

        simulator.runSimulator();
    }
}
