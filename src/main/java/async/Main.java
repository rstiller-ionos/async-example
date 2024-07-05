package async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

  class JobContext {
    Long jobId;
    AsynchronousSocketChannel channel;
    InetSocketAddress address;
    String result;
    public JobContext(Long jobId) {
      this.jobId = jobId;
    }
  }
  
  public static void main(String[] args) {
    new Main().start();
  }

  void start() {
    var threadPool = Executors.newFixedThreadPool(1);
    
    for (long i = 0L; i < 1024L; i++) {
      var ctx = new JobContext(i);
      CompletableFuture
        .supplyAsync(() -> ctx, threadPool)
        .thenApply(this::logContext)
        .thenCombine(sendRequest(ctx), (a, b) -> a)
        .orTimeout(1, TimeUnit.SECONDS)
        .whenComplete(this::cleanup)
        .whenComplete(this::summarize);
    }

  }
  
  JobContext logContext(JobContext ctx) {
    System.out.println("starting JobContext.jobId = " + ctx.jobId);
    return ctx;
  }

  CompletableFuture<JobContext> sendRequest(JobContext ctx) {
    var future = new CompletableFuture<JobContext>();
    
    System.out.println("sending request for JobContext.jobId = " + ctx.jobId);

    ctx.address = new InetSocketAddress("127.0.0.1", 8080);
    try {
      ctx.channel = AsynchronousSocketChannel.open();
      ctx.channel.connect(ctx.address, ctx, new CompletionHandler<Void, JobContext>() {
        @Override
        public void completed(Void result, JobContext attachment) {
          System.out.println("connected to server for JobContext.jobId = " + ctx.jobId);
          ctx.channel.write(ByteBuffer.wrap(("GET /" + (ctx.jobId % 4) + "s HTTP/1.1\nHost: localhost\n\n").getBytes()), 5, TimeUnit.SECONDS, attachment, new CompletionHandler<Integer, JobContext>() {
            @Override
            public void completed(Integer result, JobContext attachment) {
              System.out.println("wrote request to server for JobContext.jobId = " + ctx.jobId);
              
              var response = ByteBuffer.allocate(1024);
              ctx.channel.read(response, 5, TimeUnit.SECONDS, attachment, new CompletionHandler<Integer, JobContext>() {
                @Override
                public void completed(Integer result, JobContext attachment) {
                  System.out.println("read " + result + " bytes from server for JobContext.jobId = " + ctx.jobId);
                  ctx.result = "Success (" + response.position() + " bytes)";
                  future.complete(ctx);
                }

                @Override
                public void failed(Throwable exc, JobContext attachment) {
                  System.out.println("failed to read response from server for JobContext.jobId = " + ctx.jobId);
                  ctx.result = "Failure";
                  future.completeExceptionally(exc);
                }
              });
            }
            @Override
            public void failed(Throwable exc, JobContext attachment) {
              System.out.println("failed to write to server for JobContext.jobId = " + ctx.jobId);
              ctx.result = "Failure";
              future.completeExceptionally(exc);
            }
          });
        }
        @Override
        public void failed(Throwable exc, JobContext attachment) {
          System.out.println("failed to connect to server for JobContext.jobId = " + ctx.jobId);
        }
      });
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }

    return future;
  }

  <E extends Throwable> void cleanup(JobContext ctx, E exception) {
    System.out.println("cleanup JobContext.jobId = " + ctx.jobId);

    try {
      ctx.channel.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  <E extends Throwable> void summarize(JobContext ctx, E exception) {
    System.out.println("finished JobContext.jobId = " + ctx.jobId + " - " + ctx.result + " - " + exception);
  }

}
