package com.solomonson.example.spring.batch.partition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by randy.solomonson on 8/7/2014.
 */
@Configuration
@EnableBatchProcessing
public class ExamplePartitionJob {
    private final Logger logger = LoggerFactory.getLogger(ExamplePartitionJob.class);

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

    @Inject
    private JobBuilderFactory jobBuilders;

    @Inject
    private StepBuilderFactory stepBuilders;

    @Bean
    public Job mainJob() {
        return jobBuilders.get("mainJob")

                //Start the job by running the main step
                .start(masterStep())

                //Optional:  Use a listener just to show stats.
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        for (StepExecution se : jobExecution.getStepExecutions()) {
                            logger.info("{}={} reads", se.getStepName(), se.getReadCount());
                        }
                    }
                })
                .build();
    }

    @Bean
    public Step masterStep(){

        //Create the thread pool that will be used.
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.afterPropertiesSet();

        return stepBuilders.get("masterStep")

                //Break the input data into a map of stepName->context.
                .partitioner("extraStep", new Partitioner() {
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

                //Define the slave step to run on all the of the contexts created above.
                .step(slaveStep())

                //Define the threading of the steps.
                //.taskExecutor(taskExecutor)

                .build();
    }

    @Bean
    public Step slaveStep() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.afterPropertiesSet();


        //A basic step
        return stepBuilders.get("slaveStep")
                .<User, User>chunk(1)
                .reader(new ItemStreamReader<User>() {
                    //Use some thread locals here to keep it all thread-safe (not sure if this is required)
                    AtomicReference<String> context=new AtomicReference<>();
                    AtomicReference<Integer> i=new AtomicReference<>();
                    //ThreadLocal<Integer> i = new ThreadLocal<>();
                    //BlockingQueue<String> q=new LinkedBlockingQueue<>();

                    @Override
                    public void open(ExecutionContext executionContext) throws ItemStreamException {
                        //The read() method doesn't give a context, so all context lookup is done here.
                        context.set(executionContext.getString("key"));
                        i.set(0);
                        logger.info("open");
                    }

                    @Override
                    public void update(ExecutionContext executionContext) throws ItemStreamException {
                        //Typically used for storing state into the context.
                    }

                    @Override
                    public void close() throws ItemStreamException {
                    }

                    @Override
                    public User read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
                        i.set(i.get() + 1);
                        if (i.get() <= 10) {
                            User user = new User(context.get()+Integer.toString(i.get()));
                            return user;
                        }
                        return null;
                    }
                })

                //Define what to do with each object being processed.
                .processor(new ItemProcessor<User, User>() {
                    @Override
                    public User process(User item) throws Exception {
                        //If you want the step execution context, do this: StepSynchronizationManager.getContext().getStepExecution().getExecutionContext()
                        logger.info("{}",item);
                        //Thread.sleep(1000);
                        return item;
                    }
                })

                //Write data to something defined in the writer itself
                .writer(new ItemWriter<User>() {
                    @Override
                    public void write(List items) throws Exception {
                    }
                })
                        .taskExecutor(taskExecutor)
                        //.throttleLimit(2)
                .build();
    }
}
