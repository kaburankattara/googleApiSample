import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * 検索語に基づいてコメントのリストを出力します。
 *
 * Prints a list of videos based on a search term.
 *
 * @author Jeremy Walker
 */
public class CommentThreads {

    /** グローバル インスタンス プロパティのファイル名。 */
    /** Global instance properties filename. */
    private static String PROPERTIES_FILENAME = "youtube.properties";

    /** HTTP トランスポートのグローバル インスタンス。 */
    /** Global instance of the HTTP transport. */
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    /** JSON ファクトリのグローバル インスタンス。 */
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = new GsonFactory();

    /** 返される動画の最大数のグローバル インスタンス (50 = ページあたりの上限)。 */
    /** Global instance of the max number of videos we want returned (50 = upper limit per page). */
    private static final long NUMBER_OF_VIDEOS_RETURNED = 50;

    /** すべての API リクエストを行うための Youtube オブジェクトのグローバル インスタンス。 */
    /** Global instance of Youtube object to make all API requests. */
    private static YouTube youtube;


    /**
     * YouTube オブジェクトを初期化して、YouTube で動画を検索します (Youtube.Search.List)。 プログラム
     * 次に、各ビデオの名前とサムネイルを出力します (最初の 50 個のビデオのみ)。
     *
     * Initializes YouTube object to search for videos on YouTube (Youtube.Search.List). The program
     * then prints the names and thumbnails of each of the videos (only first 50 videos).
     *
     * @param args command line args.
     */
    public static void main(String[] args) {
        // Read the developer key from youtube.properties
        Properties properties = new Properties();
        try {
            InputStream in = CommentThreads.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            /*
             * YouTube オブジェクトは、すべての API リクエストを行うために使用されます。 最後の引数は必須ですが、
             * HttpRequest の初期化時に何も初期化する必要がないため、オーバーライドします。
             * インターフェースを変更し、no-op 機能を提供します。
             *
             *  The YouTube object is used to make all API requests. The last argument is required, but
             * because we don't need anything initialized when the HttpRequest is initialized, we override
             * the interface and provide a no-op function.
             */
            youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {}
            }).setApplicationName("youtube-cmdline-commentThreads-sample").build();

            List<String> part = new ArrayList<>();
            part.add("snippet");
            part.add("replies");
            YouTube.CommentThreads.List commentThreads = youtube.commentThreads().list(part);
            /*
             * Google Developer Console から API キーを設定することが重要です。
             * 認証されていないリクエスト (次のリンクの [認証情報] タブにあります:
             * console.developers.google.com/)。 これは良い習慣であり、クォータを増やしました。
             *
             * It is important to set your API key from the Google Developer Console for
             * non-authenticated requests (found under the Credentials tab at this link:
             * console.developers.google.com/). This is good practice and increased your quota.
             */
            String apiKey = properties.getProperty("youtube.apikey");
            commentThreads.setKey(apiKey);


            // ユーザーからクエリ用語を取得します
            // Get query term from user.
            String queryTerm = getInputQuery();
            commentThreads.setSearchTerms(queryTerm);

            /*
             * 動画のみを検索しています (プレイリストやチャンネルは検索していません)。 もし私たちが探していたら
             * さらに、"video,playlist,channel" のような文字列として追加します。
             *
             *  We are only searching for videos (not playlists or channels). If we were searching for
             * more, we would add them as a string like this: "video,playlist,channel".
             */
//            commentThreads.setChannelId("UCeLzT-7b2PBcunJplmWtoDg");
            commentThreads.setVideoId("um9_NWttXA4");

            /*
             * このメソッドは、返される情報を必要なフィールドのみに減らし、呼び出しをより効率的にします。
             *
             *  This method reduces the info returned to only the fields we need and makes calls more
             * efficient.
             */
//            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
//            commentThreads.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
            CommentThreadListResponse commentThreadResponse = commentThreads.execute();

            List<CommentThread> searchResultList = commentThreadResponse.getItems();

            if (searchResultList != null) {
                prettyPrint(searchResultList.listIterator(), queryTerm);
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /*
     * ターミナル経由でユーザーからのクエリ ターム (文字列) を返します。
     *
     * Returns a query term (String) from user via the terminal.
     */
    private static String getInputQuery() throws IOException {

        String inputQuery = "";

        System.out.print("Please enter a search term: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        inputQuery = bReader.readLine();

        if (inputQuery.length() < 1) {
            // 何も入力しない場合は、デフォルトで「YouTube Developers Live」になります。
            // If nothing is entered, defaults to "YouTube Developers Live."
            inputQuery = "YouTube Developers Live";
        }
        return inputQuery;
    }

    /*
     * Iterator 内のすべての SearchResults を出力します。 印刷された各行には、タイトル、ID、およびサムネイルが含まれます。
     *
     * Prints out all SearchResults in the Iterator. Each printed line includes title, id, and
     * thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrint(Iterator<CommentThread> iteratorCommentThreads, String query) {

        System.out.println("\n=============================================================");
        System.out.println(
                "   First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorCommentThreads.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorCommentThreads.hasNext()) {

            CommentThread commentThread = iteratorCommentThreads.next();
            Comment topLevelComment = commentThread.getSnippet().getTopLevelComment();

            // 種類がビデオであることを再確認します。
            // Double checks the kind is video.
            if (commentThread.getKind().equals("youtube#commentThread")) {

                System.out.println(" Video Id:" + commentThread.getSnippet().getVideoId());
                System.out.println(" Channel Id: " + commentThread.getSnippet().getChannelId());
                System.out.println(" TopLevel Comment Id: " + commentThread.getId());
                System.out.println(" TopLevel Comment: " + topLevelComment.getSnippet().getTextDisplay());
                System.out.println(" いいね: " + topLevelComment.getSnippet().getLikeCount());
                if (commentThread.getReplies() != null) {
                    for (Comment comment : commentThread.getReplies().getComments()) {
                        System.out.println(" Comment Id: " + comment.getId());
                        System.out.println(" Author Display Name: " + comment.getSnippet().getAuthorDisplayName());
                        System.out.println(" コメント: " + comment.getSnippet().getTextDisplay());
                        System.out.println(" いいね: " + comment.getSnippet().getLikeCount());
                    }
                }
                System.out.println("\n-------------------------------------------------------------\n");
            }
        }
    }
}