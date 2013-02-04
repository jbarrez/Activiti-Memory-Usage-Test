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

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Joram Barrez
 */
public class StatisticsSpitterThread extends Thread {
  
  private static final Logger logger = LoggerFactory.getLogger(StatisticsSpitterThread.class);
  
  private String[] processes;
  private int secondsBetweenStatistics;
  private TaskService taskService;
  private RuntimeService runtimeService;
  private HistoryService historyService;
  
  private boolean running;
  
  public StatisticsSpitterThread(ProcessEngine processEngine, String[] processes, int secondsBetweenStatistics) {
    this.taskService = processEngine.getTaskService();
    this.runtimeService = processEngine.getRuntimeService();
    this.historyService = processEngine.getHistoryService();
    this.processes = processes;
    this.secondsBetweenStatistics = secondsBetweenStatistics;
  }
  
  public void run() {
    running = true;
    while (running) {
      
      logger.info("[STATISTICS] =============================================");
      long taskCount = taskService.createTaskQuery().count();
      logger.info("[STATISTICS] Uncompleted tasks : " + taskCount);
  
      long processInstanceCount = runtimeService.createProcessInstanceQuery().count();
      logger.info("[STATISTICS] Uncompleted process instances : " + processInstanceCount);
      
      long historicTasksCount = historyService.createHistoricTaskInstanceQuery().finished().count();
      logger.info("[STATISTICS] Historical tasks : " + historicTasksCount);
      
      long historicProcessInstanceCount = historyService.createHistoricProcessInstanceQuery().finished().count();
      logger.info("[STATISTICS] Historical process instances : " + historicProcessInstanceCount);
      
      for (String process : processes) {
        long historicSpecificProcessInstanceCount = historyService.createHistoricProcessInstanceQuery().processDefinitionKey(process).finished().count();
        logger.info("[STATISTICS] Historical process instances '" + process + "' : " + historicSpecificProcessInstanceCount);
      }
      logger.info("[STATISTICS] =============================================");
      
      try {
        Thread.sleep(secondsBetweenStatistics * 1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      
    }
    
  }

  
  public boolean isRunning() {
    return running;
  }
  
  public void halt() {
    running = false;
  }

}
