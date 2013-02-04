package org.activiti;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.activiti.engine.IdentityService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.User;
import org.activiti.thread.CompleteTasksThread;
import org.activiti.thread.StartProcessInstancesThread;
import org.activiti.thread.StatisticsSpitterThread;
import org.slf4j.Logger;


/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Joram Barrez
 */
public class MemoryUsage {
  
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MemoryUsage.class);
  
  private static final String[] PROCESSES = {
    "process_async_tasks",
    "process_exclusive_gw",
    "process_four_tasks",
    "process_scripting",
    "process_subprocess_with_timer"
  };
        
  
  public static void main(String[] args) {
    logger.info("Booting Activiti Process Engine ...");
    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    registerShutdownHook(processEngine);
    logger.info("Booting Activiti Process Engine DONE");
    
    logger.info("Deploying test processes...");
    RepositoryService repositoryService = processEngine.getRepositoryService();
    for (String process : PROCESSES) {
      repositoryService.createDeployment()
        .addClasspathResource(process + ".bpmn")
        .deploy();
    }
    logger.info("Deploying test processes DONE");
    
    // Add test users
    int nrOfUsers = askForIntegerInput("How many test users do you want?");
    logger.info("Creating test users ...");
    IdentityService identityService = processEngine.getIdentityService();
    for (int i=0; i<nrOfUsers; i++) {
      User user = identityService.newUser("testUser " + i);
      identityService.saveUser(user);
    }
    logger.info("Creating test users DONE");
    
    // Ask for input
    int nrOfMinutes = askForIntegerInput("How many minutes do you want to run this program?");
    int nrOfProcessInstancePerMinute = askForIntegerInput("How many process instances do you want to start each minute?");
    int maxDelaySecondsBetweenTaskChecks = askForIntegerInput("What is the maximum wait time in seconds between two task checks of the same user?");
    int secondsBetweenStatistics = askForIntegerInput("What do you want for the duration in seconds between two engine statistics output?");
    
    // Create all threads
    final StartProcessInstancesThread startProcessInstancesThread = new StartProcessInstancesThread(processEngine, PROCESSES, nrOfUsers, nrOfProcessInstancePerMinute);
    final StatisticsSpitterThread statisticsThread = new StatisticsSpitterThread(processEngine, PROCESSES, secondsBetweenStatistics);
    final List<CompleteTasksThread> completeTasksThreads = new ArrayList<CompleteTasksThread>(nrOfUsers);
    for (int i=0; i<nrOfUsers; i++) {
      completeTasksThreads.add(new CompleteTasksThread(processEngine, "testUser" + i, maxDelaySecondsBetweenTaskChecks));
    }
    
    // Start general timer
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      public void run() {
        logger.info("Stopping all threads");
        statisticsThread.halt();
        startProcessInstancesThread.halt();
        for (CompleteTasksThread t : completeTasksThreads) {
          t.halt();
        }
        logger.info("All threads stopped");
      }
    }, nrOfMinutes * 60 * 1000);
    
    // Start all threads
    statisticsThread.start();
    startProcessInstancesThread.start();
    for (CompleteTasksThread t : completeTasksThreads) {
      t.start();
    }
    
    // Wait for all threads to complete
    try {
      startProcessInstancesThread.join();
      statisticsThread.join();
      for (CompleteTasksThread t : completeTasksThreads) {
        t.join();
      }
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    
    System.gc();
    askForInput("Enter any key to quit");
    System.gc();
    
    logger.info("Finished!");
    System.exit(0);
  }
  
  public static int askForIntegerInput(String text) {
    String input = askForInput(text);
    return Integer.valueOf(input);
  }
  
  
  @SuppressWarnings("resource")
  public static String askForInput(String text) {
      logger.info(text);

      Scanner scanner = new Scanner(System.in);
      String input = scanner.nextLine();
      return input;
  }
  
  private static void registerShutdownHook(final ProcessEngine processEngine) {
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        if (processEngine != null) {
          processEngine.close();
          logger.info("Process engine close");
        }
      }
    });
  }

}
