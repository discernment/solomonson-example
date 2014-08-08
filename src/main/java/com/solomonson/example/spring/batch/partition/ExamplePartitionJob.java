package com.solomonson.example.spring.batch.partition;

import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by randy.solomonson on 8/7/2014.
 */
@Configuration
@EnableBatchProcessing
public class ExamplePartitionJob {
    public static class User{
        private String name;
        public User(String name){
            this.name=name;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    @Autowired
    private JobBuilderFactory jobBuilders;

    @Autowired
    private StepBuilderFactory stepBuilders;

    @Bean
    public Job mainJob() {
        return jobBuilders.get("mainJob")
                .start(masterStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        for (StepExecution se:jobExecution.getStepExecutions()){
                            System.out.printf("%s=%d reads\n", se.getStepName(), se.getReadCount());
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step masterStep(){
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(2);
        taskExecutor.afterPropertiesSet();

        return stepBuilders.get("masterStep")
                .partitioner(slaveStep())
                .partitioner("slaveStep", new Partitioner() {
                    @Override
                    public Map<String, ExecutionContext> partition(int gridSize) {
                        Map<String, ExecutionContext> contexts = new HashMap<>();
                        ExecutionContext executionContext = new ExecutionContext();
                        executionContext.put("key", "a");
                        contexts.put("a", executionContext);

                        executionContext = new ExecutionContext();
                        executionContext.put("key", "b");
                        contexts.put("b", executionContext);
                        return contexts;
                    }
                })
                .taskExecutor(taskExecutor)
                .build();
    }

    @Bean
    public Step slaveStep() {

        return stepBuilders.get("slaveStep")
                .<User, User>chunk(1)
                .reader(new ItemStreamReader<User>() {
                    ThreadLocal<Integer> i = new ThreadLocal<>();

                    @Override
                    public void open(ExecutionContext executionContext) throws ItemStreamException {
                        i.set(0);
                    }

                    @Override
                    public void update(ExecutionContext executionContext) throws ItemStreamException {

                    }

                    @Override
                    public void close() throws ItemStreamException {
                    }

                    @Override
                    public User read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                        i.set(i.get() + 1);
                        if (i.get() <= 10) {
                            User user = new User(Integer.toString(i.get()));
                            return user;
                        }
                        return null;
                    }
                })
                .writer(new ItemWriter<User>() {
                    @Override
                    public void write(List items) throws Exception {
                    }
                })
                        //.taskExecutor(taskExecutor)
                        //.throttleLimit(2)
                .build();
    }

}
