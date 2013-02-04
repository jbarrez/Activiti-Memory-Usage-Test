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
package org.activiti.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;

/**
 * @author Joram Barrez
 */
public class StartProcessInstancesThread extends Thread {

  private boolean running;

  private RuntimeService runtimeService;
  private String[] processes;
  private int nrOfUsers;
  private long delay;
  private Random random;

  public StartProcessInstancesThread(ProcessEngine processEngine, String[] processes, int nrOfUsers, int nrOfProcessInstancePerMinute) {
    this.runtimeService = processEngine.getRuntimeService();
    this.processes = processes;
    this.nrOfUsers = nrOfUsers;
    this.delay = (60L * 1000L) / (long) nrOfProcessInstancePerMinute;
    this.random = new Random();
  }

  @Override
  public void run() {
    running = true;


    while (running) {
      String process = processes[random.nextInt(processes.length)];
      
      Map<String, Object> variables = new HashMap<String, Object>();
      for (int i = 0; i < 10; i++) {
        variables.put("assignee" + i, "testUser" + random.nextInt(nrOfUsers));
      }
      variables.put("number", random.nextInt(10)); // used by some processes
      runtimeService.startProcessInstanceByKey(process, variables);
      
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void halt() {
    running = false;
  }

}
